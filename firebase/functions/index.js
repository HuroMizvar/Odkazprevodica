/**
 * Serverová časť aplikácie Odkaz pre vodiča (Firebase Cloud Functions).
 *
 * 1. onMessageCreated – nový odkaz pre ŠPZ:
 *    - ak ŠPZ nikto nesleduje → odkaz sa zmaže (odosielateľ sa to nedozvie)
 *    - nad limit (3 na ŠPZ / 20 za 24 h od odosielateľa) → zmaže sa
 *    - inak push notifikácia každému, kto ŠPZ sleduje (okrem tých, čo odosielateľa zablokovali)
 * 2. onMessageUpdated – majiteľ odpovedal / označil ako prečítané:
 *    - prepíše sa do odoslaných u odosielateľa + push s odpoveďou
 * 3. cleanup – každý deň zmaže odkazy staršie ako 30 dní
 * 4. updateStats – 1. dňa v mesiaci spočíta unikátne ŠPZ v aplikácii
 *    (ŠPZ sledovaná viacerými ľuďmi sa ráta iba raz)
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 5 });

const db = admin.firestore();

const LIMIT_PER_PLATE = 3;
const LIMIT_PER_DAY = 20;
const RETENTION_DAYS = 30;
const DAY_MS = 24 * 60 * 60 * 1000;

// Texty musia sedieť s aplikáciou (app/src/main/res/values/strings.xml).
const MESSAGE_TEXTS = {
  lights: "Svietia vám svetlá",
  window: "Máte otvorené okno",
  door: "Máte zle zatvorené dvere alebo kufor",
  tire: "Máte podfúknutú alebo prázdnu pneumatiku",
  fault: "Na vašom aute je závada",
  fell_off: "Niečo vám vypadlo z auta alebo zostalo na streche",
  child_animal: "Vo vozidle zostalo zviera alebo dieťa",
  blocking: "Blokujete výjazd alebo vjazd",
  tow: "Hrozí vám odťah alebo pokuta",
  damage: "Vaše auto bolo poškodené",
  alarm: "Spustil sa vám alarm",
};
const URGENT = new Set(["child_animal", "tow", "blocking"]);

const REPLY_TEXTS = {
  checking: "Ďakujem, skontrolujem to",
  sorry_checking: "Ospravedlňujem sa, skontrolujem to",
  resolved: "Ďakujem, už je to vyriešené",
  sorry_resolved: "Ospravedlňujem sa, už je to vyriešené",
};

function displayPlate(p) {
  return p.length > 2 ? `${p.slice(0, 2)}-${p.slice(2)}` : p;
}

/** Pošle dátovú push správu; neplatný token zmaže. */
async function sendPush(uid, data) {
  const userSnap = await db.collection("users").doc(uid).get();
  const token = userSnap.get("fcmToken");
  if (!token) return;
  try {
    await admin.messaging().send({
      token,
      data,
      android: { priority: "high", ttl: 6 * 60 * 60 * 1000 },
    });
  } catch (err) {
    const code = err && err.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-argument") {
      await userSnap.ref.update({ fcmToken: admin.firestore.FieldValue.delete() }).catch(() => {});
    } else {
      logger.warn("push zlyhal", { uid, code });
    }
  }
}

/** true = odosielateľ prekročil limit (vrátane tohto odkazu). */
async function overLimit(senderUid, plate) {
  try {
    const since = admin.firestore.Timestamp.fromMillis(Date.now() - DAY_MS);
    const recent = await db.collectionGroup("messages")
      .where("senderUid", "==", senderUid)
      .where("createdAt", ">=", since)
      .get();
    const toPlate = recent.docs.filter((d) => d.ref.parent.parent.id === plate).length;
    return recent.size > LIMIT_PER_DAY || toPlate > LIMIT_PER_PLATE;
  } catch (err) {
    // Index sa po prvom nasadení ešte vytvára – radšej doručiť ako zahodiť.
    logger.warn("kontrola limitu zlyhala", err);
    return false;
  }
}

exports.onMessageCreated = onDocumentCreated("plates/{plate}/messages/{id}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const { plate, id } = event.params;
  const msg = snap.data();
  const text = MESSAGE_TEXTS[msg.templateId];

  if (!text || await overLimit(msg.senderUid, plate)) {
    await snap.ref.delete();
    return;
  }

  const members = await db.collection("plates").doc(plate).collection("members").get();
  if (members.empty) {
    // ŠPZ nikto nesleduje → zahodiť bez stopy.
    await snap.ref.delete();
    return;
  }

  await Promise.all(members.docs.map(async (m) => {
    const memberUid = m.id;
    const user = await db.collection("users").doc(memberUid).get();
    const blocked = user.get("blocked") || {};
    if (blocked[msg.senderUid] !== undefined) return;
    await sendPush(memberUid, {
      type: "message",
      title: `ŠPZ ${displayPlate(plate)}`,
      body: text,
      plate,
      messageId: id,
      urgent: URGENT.has(msg.templateId) ? "1" : "0",
    });
  }));
});

exports.onMessageUpdated = onDocumentUpdated("plates/{plate}/messages/{id}", async (event) => {
  const before = event.data.before.data() || {};
  const after = event.data.after.data() || {};
  const { plate, id } = event.params;

  const replyChanged = after.reply && after.reply !== before.reply;
  const readChanged = after.readConfirmed === true && before.readConfirmed !== true;
  if (!replyChanged && !readChanged) return;
  if (!after.senderUid) return;

  // Odosielateľ uvidí odpoveď vo svojich odoslaných.
  await db.collection("users").doc(after.senderUid).collection("sent").doc(id).set({
    plate,
    templateId: after.templateId,
    createdAt: after.createdAt,
    reply: after.reply || null,
    readConfirmed: after.readConfirmed === true,
  }, { merge: true });

  if (replyChanged && REPLY_TEXTS[after.reply]) {
    await sendPush(after.senderUid, {
      type: "reply",
      title: `Odpoveď vodiča · ${displayPlate(plate)}`,
      body: REPLY_TEXTS[after.reply],
      messageId: `${id}-reply`,
      urgent: "0",
    });
  }
});

/** Zmaže dokumenty staršie ako limit v dávkach po 400. */
async function deleteOlderThan(collectionId, cutoff) {
  let total = 0;
  for (;;) {
    const page = await db.collectionGroup(collectionId).where("createdAt", "<", cutoff).limit(400).get();
    if (page.empty) break;
    const batch = db.batch();
    page.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    total += page.size;
  }
  return total;
}

exports.cleanup = onSchedule({ schedule: "every day 03:00", timeZone: "Europe/Bratislava" }, async () => {
  const cutoff = admin.firestore.Timestamp.fromMillis(Date.now() - RETENTION_DAYS * DAY_MS);
  const messages = await deleteOlderThan("messages", cutoff);
  const sent = await deleteOlderThan("sent", cutoff);
  logger.info("mazanie starých odkazov", { messages, sent });
});

// ---- Štatistika: počet unikátnych ŠPZ ----
//
// Dokument stats/global (čítajú ho všetci prihlásení, zapisuje iba server, raz za mesiac):
//   uniquePlates          – počet ŠPZ, ktoré sleduje aspoň 1 človek
//   registeredVehiclesSK  – počet vozidiel v SR; meníš ho ručne vo Firebase konzole
//                           (Firestore Database → stats → global)

const REGISTERED_VEHICLES_SK = 3700000; // počiatočná hodnota, neskôr sa mení v konzole
const statsRef = () => db.collection("stats").doc("global");

/** Presný prepočet: spočíta ŠPZ, ktoré majú aspoň jedného sledovateľa. */
async function recountStats() {
  // po stránkach, aby to zvládlo aj milióny záznamov
  const plates = new Set();
  let last = null;
  for (;;) {
    let q = db.collectionGroup("members").orderBy(admin.firestore.FieldPath.documentId()).select().limit(10000);
    if (last) q = q.startAfter(last);
    const page = await q.get();
    if (page.empty) break;
    page.docs.forEach((d) => plates.add(d.ref.parent.parent.id));
    last = page.docs[page.docs.length - 1];
  }
  const ref = statsRef();
  const snap = await ref.get();
  const data = {
    uniquePlates: plates.size,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  if (!snap.exists || snap.get("registeredVehiclesSK") === undefined) {
    data.registeredVehiclesSK = REGISTERED_VEHICLES_SK;
  }
  await ref.set(data, { merge: true });
  return plates.size;
}

// Raz za mesiac (1. deň o 4:00). Prepočet číta všetkých sledovateľov:
// pri 3,7 mil. ŠPZ ~3,7 mil. čítaní mesačne (odhad ~1–2 €), bez nákladov
// pri každej registrácii. Ručné spustenie v Cloud Shell:
//   gcloud scheduler jobs run firebase-schedule-updateStats-europe-west1 --location=europe-west1
exports.updateStats = onSchedule({
  schedule: "0 4 1 * *",
  timeZone: "Europe/Bratislava",
  timeoutSeconds: 540,
  memory: "1GiB",
}, async () => {
  const uniquePlates = await recountStats();
  logger.info("štatistika ŠPZ", { uniquePlates });
});
