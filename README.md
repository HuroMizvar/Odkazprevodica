# Odkaz pre vodiča

Android aplikácia na anonymné posielanie preddefinovaných odkazov vodičom podľa ŠPZ
(svietia svetlá, otvorené okno, blokujete výjazd…).

- Balík: `sk.odkazprevodica.app`
- Technológia: Kotlin, Jetpack Compose, Material 3
- Dizajn: prevedenie **A1 · Signál** (tmavé, jantárová)
- Stav: **v0.2 – funkcie v telefóne, bez servera**

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

## Ďalšie kroky

| # | Krok |
|---|---|
| 1 | Firebase: Auth (Google), Firestore, Cloud Messaging |
| 2 | Prihlásenie cez Google, pseudonym na serveri |
| 3 | Cloud Functions: doručenie, limity na serveri, mazanie po 30 dňoch, pozvánky a spory o ŠPZ |
| 4 | Push notifikácie |
| 5 | Písmo Archivo, finálna ikona |
| 6 | Play Console: zásady ochrany súkromia, formulár bezpečnosti údajov, uzavreté testovanie |

`app/debug.keystore` je verejný testovací podpis (nie pre Google Play), aby sa nové
testovacie APK dali inštalovať cez staršie.

## Pravidlá produktu

- Iba preddefinované odkazy a odpovede, žiadny voľný text.
- Odosielateľ nevidí, či ŠPZ má konto, ani či bol odkaz doručený alebo prečítaný.
- Odkaz na ŠPZ bez konta sa zahodí.
- Odkazy sa automaticky mažú po 30 dňoch.
- Ďalší sledovateľ ŠPZ iba na pozvánku.
