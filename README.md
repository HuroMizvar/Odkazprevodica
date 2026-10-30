# Odkaz pre vodiča

Android aplikácia na anonymné posielanie preddefinovaných odkazov vodičom podľa ŠPZ
(svietia svetlá, otvorené okno, blokujete výjazd…).

- Balík: `sk.odkazprevodica.app`
- Technológia: Kotlin, Jetpack Compose, Material 3
- Dizajn: prevedenie **A1 · Signál** (tmavé, jantárová)
- Stav: **v0.3 – odkazy medzi zariadeniami cez Firebase, push notifikácie**
- Zapnutie servera: pozri **[FIREBASE.md](FIREBASE.md)** (bez neho beží testovací režim v jednom telefóne)

## Spustenie

APK zostaví GitHub Actions pri každom commite do `main` (záložka Actions / Releases).
Lokálne: otvor priečinok v Android Studio a spusti Run ▶.

## Čo je hotové

| Funkcia | Stav |
|---|---|
| Domov | Autá z úložiska, počet neprečítaných, posledný odkaz |
| Pridať auto | ŠPZ + názov, potvrdenie používania, max. 20, kontrola duplicity |
| Schránka auta | Zoznam odkazov, premenovať, odobrať (zmaže aj odkazy) |
| Nový odkaz | Ručne alebo **skenom ŠPZ** (CameraX + ML Kit, v telefóne), 11 šablón, limity 3/ŠPZ/deň a 20/deň |
| Prijatý odkaz | Odpoveď (s bezpečnostnou výstrahou), prečítané, blokovať, nahlásiť |
| Odoslané | História bez stavu doručenia |
| Nastavenia | Pseudonym, blokovaní, zásady súkromia, vymazanie údajov |
| Úložisko | V telefóne (SharedPreferences), automatické mazanie po 30 dňoch |

**Test bez servera:** odkaz poslaný na vlastnú ŠPZ príde do vlastnej schránky.
Pri prvom spustení sa vytvoria 3 ukážkové autá s odkazmi.

Šablóny: `app/src/main/java/sk/odkazprevodica/app/data/Templates.kt` · Texty: `app/src/main/res/values/strings.xml`

## Server (priečinok `firebase/`)

| Súbor | Obsah |
|---|---|
| `firestore.rules` | Kto smie čo čítať a zapisovať (odosielateľ odkaz nevie prečítať) |
| `firestore.indexes.json` | Indexy pre limity a mazanie |
| `functions/index.js` | Doručenie + push, zahodenie bez konta, limity, odpovede, mazanie po 30 dňoch |

## Ďalšie kroky

| # | Krok |
|---|---|
| 1 | Písmo Archivo, finálna ikona |
| 2 | Zásady ochrany súkromia (web) + zmazanie konta cez web |
| 3 | Play Console: formulár bezpečnosti údajov, uzavreté testovanie |

`app/debug.keystore` je verejný testovací podpis (nie pre Google Play), aby sa nové
testovacie APK dali inštalovať cez staršie.

## Pravidlá produktu

- Iba preddefinované odkazy a odpovede, žiadny voľný text.
- Odosielateľ nevidí, či ŠPZ má konto, ani či bol odkaz doručený alebo prečítaný.
- Odkaz na ŠPZ bez konta sa zahodí.
- Odkazy sa automaticky mažú po 30 dňoch.
- Ďalší sledovateľ ŠPZ iba na pozvánku.
