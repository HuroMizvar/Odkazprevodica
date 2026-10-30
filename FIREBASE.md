# Zapnutie servera (Firebase)

Bez Firebase beží aplikácia v testovacom režime – všetko iba v jednom telefóne.
Po týchto krokoch idú odkazy medzi telefónmi a chodia push notifikácie.

Čas: približne 20–30 minút. Potrebuješ Google účet a platobnú kartu (plán Blaze).

## 1. Projekt

1. Otvor <https://console.firebase.google.com> → **Create a project / Vytvoriť projekt**.
2. Názov: `odkaz-pre-vodica`. Google Analytics môžeš vypnúť.
3. Zapíš si **Project ID** (zobrazí sa pod názvom, napr. `odkaz-pre-vodica-1a2b3`).

## 2. Plán Blaze + strop výdavkov

Push notifikácie posiela serverová funkcia (Cloud Functions), ktorá beží iba na pláne Blaze.

1. Vľavo dole **Upgrade** → **Blaze** → pridaj platobný účet (karta).
2. Pri nastavení zadaj rozpočtové upozornenie (budget alert), napr. **1 €**.

Pri malom počte používateľov sa zmestíš do bezplatného limitu (odhad: 0 €).
Rozpočtové upozornenie výdavky nezastaví, iba ťa upozorní e-mailom.

## 3. Prihlásenie bez e-mailu

**Build → Authentication → Get started → Sign-in method → Anonymous → Enable → Save.**

## 4. Databáza

**Build → Firestore Database → Create database**
- Lokalita: **europe-west1 (Belgium)** – dáta zostanú v EÚ
- Režim: **Production mode** (pravidlá nahráme v kroku 6)

## 5. Android aplikácia

1. **Project settings (ozubené koliesko) → Your apps → Android ikona**
2. Package name: `sk.odkazprevodica.app` (presne takto)
3. **Register app → Download google-services.json**
4. Nahraj súbor do repozitára do priečinka **`app/`**
   (na GitHube otvor priečinok `app` → Add file → Upload files).

Súbor `google-services.json` môže byť vo verejnom repozitári – obsahuje iba identifikátory
projektu, nie heslá. Dáta chránia pravidlá databázy (`firebase/firestore.rules`).

GitHub potom sám zostaví APK s pripojením na server.

## 6. Nahratie serverovej časti (pravidlá + funkcie)

Bez inštalácie čohokoľvek do počítača – cez Google Cloud Shell v prehliadači:

1. Otvor <https://console.cloud.google.com/?cloudshell=true>
   a hore vyber projekt `odkaz-pre-vodica`.
2. Dole sa otvorí terminál. Vlož tieto riadky (namiesto `PROJECT_ID` tvoje Project ID z kroku 1):

```bash
git clone https://github.com/HuroMizvar/odkazprevodica
cd odkazprevodica/firebase/functions && npm install && cd ..
npx -y firebase-tools@latest deploy --project PROJECT_ID
```

- Ak si vypýta prihlásenie: `npx -y firebase-tools@latest login --no-localhost` a postupuj podľa odkazu.
- Prvé nasadenie zapína služby Google Cloud a trvá 3–5 minút.
- Ak skončí chybou o oprávneniach („permissions… propagating“), počkaj 5 minút a spusti posledný riadok znova.

Pri ďalších zmenách servera stačí:

```bash
cd ~/odkazprevodica && git pull && cd firebase/functions && npm install && cd .. && npx -y firebase-tools@latest deploy --project PROJECT_ID
```

## 7. Test na dvoch telefónoch

1. Nainštaluj novú APK na oba telefóny (aj požičaný).
2. Telefón A: **Pridať auto** so svojou ŠPZ.
3. Telefón B: **Poslať odkaz** na túto ŠPZ.
4. Telefón A dostane notifikáciu s veľkým písmom → ťuknutie otvorí schránku auta → odpoveď.
5. Telefón B uvidí odpoveď v **Odoslané** a dostane notifikáciu.

## Xiaomi / HyperOS – aby notifikácie chodili aj pri zatvorenej aplikácii

**Nastavenia → Aplikácie → Odkaz pre vodiča:**
- **Automatické spustenie (Autostart)**: zapnúť
- **Šetrič batérie**: Bez obmedzení
- **Upozornenia**: povoliť, vrátane **Plávajúce upozornenia**
