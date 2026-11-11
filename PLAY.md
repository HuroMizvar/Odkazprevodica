# Zverejnenie v Google Play – krok po kroku

Osobný vývojársky účet ⇒ pred zverejnením je povinné **uzavreté testovanie:
aspoň 12 testerov počas 14 dní bez prerušenia**. Až potom Google odomkne „Production“.

| Fáza | Čo | Kto | Čas |
|---|---|---|---|
| A | Kľúč na podpis (upload key) + GitHub Secrets | ty, podľa návodu | 15 min |
| B | Zásady ochrany súkromia na webe (GitHub Pages) | ty | 5 min |
| C | Aplikácia v Play Console + formuláre | ty, texty sú nižšie | 60 min |
| D | Uzavreté testovanie – 12 testerov, 14 dní | ty + testeri | 14 dní |
| E | Žiadosť o produkciu → zverejnenie | ty | 1–7 dní kontrola Google |

---

## A. Kľúč na podpis (raz navždy)

Google Play prijíma iba podpísaný balík (AAB). Kľúč vytvoríš v Cloud Shell.

1. Otvor <https://console.cloud.google.com/?cloudshell=true&project=odkaz-pre-vodica>
2. Vymysli si **heslo** (min. 8 znakov, bez medzier a úvodzoviek) a zapíš si ho bezpečne.
3. Do terminálu vlož (HESLO nahraď svojím heslom):

```bash
keytool -genkeypair -keystore ~/upload-key.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Odkaz pre vodica, C=SK" -storepass 'HESLO' -keypass 'HESLO' && base64 -w0 ~/upload-key.jks > ~/upload-key.txt && cloudshell download ~/upload-key.jks && cloudshell download ~/upload-key.txt
```

4. Prehliadač stiahne 2 súbory:
   - `upload-key.jks` – **záloha kľúča**. Ulož ho bezpečne (napr. Google Drive) spolu s heslom.
   - `upload-key.txt` – ten istý kľúč ako text, pre GitHub.
5. GitHub → repozitár → **Settings → Secrets and variables → Actions → New repository secret**. Vytvor 4 secrets:

| Name | Secret |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | celý obsah súboru `upload-key.txt` (otvor v Poznámkovom bloku, Ctrl+A, Ctrl+C) |
| `RELEASE_KEYSTORE_PASSWORD` | tvoje heslo |
| `RELEASE_KEY_ALIAS` | `upload` |
| `RELEASE_KEY_PASSWORD` | tvoje heslo |

6. Nahraj nový `build-apk.yml` (dostaneš v správe) – po ďalšom builde bude v Actions okrem APK aj
   **google-play-aab-N** (ZIP s `.aab` pre Google Play).

Ak kľúč stratíš: pri zapnutom „Play App Signing“ (krok C5) ho Google vie vymeniť cez podporu.

---

## B. Zásady ochrany súkromia (GitHub Pages)

1. GitHub → repozitár → **Settings → Pages**
2. Source: **Deploy from a branch** · Branch: **main** · Folder: **/docs** → **Save**
3. Po 1–2 minútach sa hore zobrazí adresa, napr. `https://huromizvar.github.io/odkazprevodica/`
4. Odkazy pre Play Console:
   - Zásady ochrany súkromia: `…/privacy.html`
   - Zmazanie konta: `…/delete-account.html`
5. V súboroch `docs/privacy.html` a `docs/delete-account.html` nahraď `[PRIEZVISKO]` svojím priezviskom
   (GitHub → súbor → ceruzka → uložiť). GDPR vyžaduje identifikáciu prevádzkovateľa.

---

## C. Play Console

### C1. Vytvorenie aplikácie
**Home → Create app**
- App name: `Odkaz pre vodiča`
- Default language: **Slovak – sk-SK**
- App or game: **App** · Free or paid: **Free**
- Odškrtni vyhlásenia → **Create app**

### C2. App content (ľavé menu „Policy → App content“)

| Formulár | Odpoveď |
|---|---|
| Privacy policy | URL `…/privacy.html` |
| App access | **All functionality is available without special access** (prihlásenie je automatické a anonymné) |
| Ads | **No, my app does not contain ads** |
| Content rating | pozri C3 |
| Target audience | **18 and over** (aplikácia pre vodičov; nie je pre deti) |
| News app | No |
| Government app | No |
| Financial features | My app doesn't provide any financial features |
| Health | No health features |
| Data safety | pozri C4 |
| Account deletion | Áno, aplikácia vytvára (anonymné) konto · URL `…/delete-account.html` |

### C3. Content rating (dotazník IARC)
- E-mail: `huromir@gmail.com` · Category: **Communication / Social** (alebo „All other app types“)
- Násilie, sex, drogy, hazard, vulgarizmy: **No**
- Can users interact or exchange content with each other? **Yes** (preddefinované odkazy medzi používateľmi)
- Does the app share the user's physical location? **No**
- Does the app allow purchases of digital goods? **No**
- Výsledok bude približne PEGI 3 / „Users interact“.

### C4. Data safety

**Úvodné otázky**
| Otázka | Odpoveď |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Which of the following methods of account creation does your app support? | **Other** – anonymné konto bez e-mailu |
| Delete account URL | `…/delete-account.html` |
| Do you provide a way for users to request that some or all of their data is deleted? | **Yes** |

**Typy údajov** (všetky: *Collected* = áno, *Shared* = nie, *Processed ephemerally* = nie, *Required*)
| Kategória → typ | Prečo | Účel (Purpose) |
|---|---|---|
| Personal info → **User IDs** | anonymný identifikátor + pseudonym | App functionality; Fraud prevention, security, and compliance |
| Messages → **Other in-app messages** | preddefinované odkazy a odpovede | App functionality |
| App activity → **Other user-generated content** | ŠPZ, ktoré si používateľ pridá | App functionality |
| Device or other IDs → **Device or other IDs** | token pre push notifikácie | App functionality |

Neoznačuj: polohu, fotky (fotka ŠPZ sa spracuje iba v telefóne), kontakty, financie, zdravie.
„Shared“ = nie: Firebase je sprostredkovateľ (service provider), to sa za zdieľanie nepočíta.

### C5. Store listing (Grow → Store presence → Main store listing)

**App name** (max 30): `Odkaz pre vodiča`

**Short description** (max 80):
```
Pošlite vodičovi anonymný odkaz podľa ŠPZ – svetlá, okno, blokovaný výjazd.
```

**Full description** (max 4000):
```
Svietia niekomu svetlá? Nechal otvorené okno? Blokuje vám výjazd? Povedzte mu to – rýchlo, slušne a anonymne.

Odkaz pre vodiča spája vodičov cez ŠPZ. Pridajte si svoje auto a keď s ním bude niečo v neporiadku, ktokoľvek vám môže poslať upozornenie priamo do telefónu.

AKO TO FUNGUJE
• Odfoťte alebo zadajte ŠPZ auta
• Vyberte, čo sa deje – napr. svietia svetlá, otvorené okno, podfúknutá pneumatika, hrozí odťah
• Majiteľ dostane upozornenie s veľkým písmom, čitateľné aj v aute

BEZPEČNE A ANONYMNE
• Bez registrácie e-mailom – konto vznikne automaticky a je anonymné
• Iba preddefinované odkazy, žiadne urážky ani voľný text
• Odosielateľ nevidí, či ŠPZ niekto používa, ani či bol odkaz doručený
• Majiteľ môže odpovedať jedným ťuknutím, zablokovať odosielateľa alebo odkaz nahlásiť
• Odkazy sa automaticky mažú po 30 dňoch
• Dáta sú uložené v EÚ, bez reklamy a bez sledovania polohy

PRE CELÚ RODINU
• Až 20 áut na jedno konto
• Rovnakú ŠPZ môže sledovať viac ľudí

ODKAZY
Svietia vám svetlá · Otvorené okno · Zle zatvorené dvere alebo kufor · Podfúknutá pneumatika · Závada na aute · Niečo vypadlo z auta · Zviera alebo dieťa vo vozidle · Blokujete výjazd · Hrozí odťah alebo pokuta · Poškodené auto · Spustený alarm

V ohrození života vždy volajte 112.
```

**Grafika** (priečinok `play/` v repozitári)
| Položka | Súbor / postup |
|---|---|
| App icon 512×512 | `play/icon-512.png` |
| Feature graphic 1024×500 | `play/feature-graphic-1024x500.png` |
| Phone screenshots (min. 2, odporúčané 4–8) | Screenshoty z Xiaomi: Domov, Nový odkaz, Prijatý odkaz, notifikácia |

**Store settings**
- App category: **Auto & Vehicles**
- Contact e-mail: `huromir@gmail.com` · Website: adresa z kroku B

### C6. Krajiny
**Production → Countries/regions → Slovakia** (neskôr môžeš pridať Česko).

---

## D. Uzavreté testovanie (14 dní)

1. **Test and release → Testing → Closed testing → Create track** (alebo použi „Alpha“)
2. **Testers → Create email list** → vlož Gmail adresy 12+ testerov (odporúčam 14–15 pre rezervu)
3. **Create new release**:
   - Pri prvom nahratí potvrď **Play App Signing** („Google manages your app signing key“)
   - Nahraj `.aab` z GitHub Actions (artefakt **google-play-aab-N**, rozbaľ ZIP)
   - Release name: napr. `1.0.N` · Release notes: `Prvá testovacia verzia`
4. **Review release → Start rollout to Closed testing**. Google kontroluje (hodiny až niekoľko dní).
5. Po schválení: **Testers → Copy link** → pošli testerom. Každý:
   - otvorí odkaz v telefóne, prihlásený tým Gmailom, ktorý si zadal
   - klikne **Become a tester** → nainštaluje z Google Play
   - **14 dní aplikáciu neodinštaluje** a neodhlási sa z testovania
6. Počas testovania môžeš nahrávať nové verzie – deň testera sa nepreruší.

## E. Produkcia

Po 14 dňoch: **Dashboard → Apply for production**. Google sa pýta na priebeh testovania
(koľko testerov, aká spätná väzba, čo ste opravili). Odpovedz konkrétne – pomôže to schváleniu.
Potom **Production → Create new release** → nahraj AAB → **Start rollout**.

---

## Dôležité rozdiely: testovacia APK vs. Google Play

| | APK z GitHubu (testovacia) | AAB pre Google Play |
|---|---|---|
| Podpis | testovací (`debug.keystore`) | tvoj upload key → Google |
| Aktualizácia v aplikácii (Nastavenia) | áno | **nie** – Play to zakazuje, aktualizuje Obchod Play |
| Inštalácia | ručne | z Obchodu Play |

Testovacia APK a verzia z Play sa navzájom **neprepíšu** (iný podpis). Pred inštaláciou z Play
testovaciu APK odinštaluj.
