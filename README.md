# Odkaz pre vodiča

Android aplikácia na anonymné posielanie preddefinovaných odkazov vodičom podľa ŠPZ
(svietia svetlá, otvorené okno, blokujete výjazd…).

- Balík: `sk.odkazprevodica.app`
- Technológia: Kotlin, Jetpack Compose, Material 3
- Dizajn: prevedenie **A1 · Signál** (tmavé, jantárová)
- Stav: **v0.1 – kostra UI s testovacími dátami**, bez backendu

## Spustenie

1. Otvor priečinok v **Android Studio** (File → Open).
2. Počkaj na Gradle sync. Ak Studio ponúkne aktualizáciu AGP alebo Kotlinu, prijmi ju.
3. Spusti na emulátore alebo telefóne (Run ▶).

## Čo je hotové

| Obrazovka | Obsah |
|---|---|
| Domov | Tlačidlo „Poslať odkaz“, zoznam mojich áut (max. 20) s počtom nových odkazov |
| Nový odkaz | Zadanie ŠPZ (normalizácia, kontrola formátu), 11 šablón, odoslanie s rovnakým potvrdením bez ohľadu na existenciu konta |
| Prijatý odkaz | Detail, 3 odpovede, bezpečnostná výstraha pred „Ďakujem, skontrolujem to“, označiť ako prečítané, blokovať, nahlásiť |

Šablóny odkazov a odpovedí: `app/src/main/java/sk/odkazprevodica/app/data/Templates.kt`
Texty (slovenčina): `app/src/main/res/values/strings.xml`

## Ďalšie kroky

| # | Krok |
|---|---|
| 1 | Firebase projekt: Auth (Google), Firestore, Cloud Messaging; `google-services.json` do `app/` (necommitovať) |
| 2 | Prihlásenie cez Google + pseudonym používateľa |
| 3 | Pridanie auta, pozvánka pre ďalšieho sledovateľa, spor o ŠPZ |
| 4 | Cloud Functions: doručenie odkazu, limity (3/ŠPZ/deň na odosielateľa), mazanie po 30 dňoch |
| 5 | Push notifikácie |
| 6 | Skenovanie ŠPZ: CameraX + ML Kit Text Recognition |
| 7 | Obrazovky Odoslané a Nastavenia (blokovaní, zmazanie konta, zásady ochrany súkromia) |
| 8 | Písmo Archivo, finálna adaptívna ikona |
| 9 | Play Console: zásady ochrany súkromia, formulár bezpečnosti údajov, uzavreté testovanie |

## Pravidlá produktu

- Iba preddefinované odkazy a odpovede, žiadny voľný text.
- Odosielateľ nevidí, či ŠPZ má konto, ani či bol odkaz doručený alebo prečítaný.
- Odkaz na ŠPZ bez konta sa zahodí.
- Odkazy sa automaticky mažú po 30 dňoch.
- Ďalší sledovateľ ŠPZ iba na pozvánku.
