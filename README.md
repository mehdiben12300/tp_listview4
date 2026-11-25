# TP Android — Gestion des contacts (Version 4)
### Persistance SQLite • Préférences utilisateur • Fragments • Google Maps

Évolution du TP V3 : les contacts sont désormais stockés dans une base **SQLite** locale (`contacts.db`, API native `SQLiteOpenHelper`, sans Room) et un écran **Paramètres** (AndroidX Preference) personnalise l'affichage.

## Lancer le projet
1. Ouvrir le dossier dans Android Studio et synchroniser Gradle.
2. (Optionnel, pour le mode Google Maps SDK) copier `secrets.properties.example` en `secrets.properties` et y mettre `MAPS_API_KEY=...`. Ce fichier est ignoré par Git : **aucune clé n'est publiée**.
3. Lancer l'application sur un émulateur ou un appareil (minSdk 24).

Tests :
```bash
./gradlew testDebugUnitTest          # tests JVM : validation, modèle, préférences
./gradlew connectedAndroidTest       # tests instrumentés du DAO (base en mémoire)
```

## Architecture
```
com.example.tp_listview
├── data/
│   ├── Contact.kt             data class (id = 0 tant que non enregistré)
│   ├── ContactContract.kt     nom de table, colonnes, CREATE TABLE, ORDER BY
│   ├── ContactDbHelper.kt     SQLiteOpenHelper singleton, onCreate (+7 contacts démo), onUpgrade
│   ├── ContactDao.kt          seule classe contenant du SQL (insert/update/delete/getAll/getById/search)
│   └── ContactValidator.kt    règles de validation (testées en JVM)
├── MainActivity.kt            activité unique, FragmentContainerView
├── ListeContactsFragment.kt   liste, filtre, FAB « + », bouton Appeler par ligne, menu Paramètres
├── DetailContactFragment.kt   création / consultation / modification / suppression
├── CarteContactFragment.kt    carte (Leaflet/OSM ou Google Maps SDK) + messages si position absente/invalide
├── ParametresFragment.kt      PreferenceFragmentCompat (res/xml/preferences.xml)
├── ContactViewModel.kt        ViewModel partagé, DAO appelé dans viewModelScope + Dispatchers.IO
├── ContactAdapter.kt          ListAdapter + DiffUtil, stableIds = _id
├── PreferencesManager.kt      lecture/conversion centralisée des préférences
└── PhotoUtils.kt              copie des photos dans filesDir/photos, affichage
```

## Schéma (V2 avec Bonus)
```sql
CREATE TABLE contacts (
    _id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nom        TEXT NOT NULL,
    prenom     TEXT NOT NULL,
    telephone  TEXT NOT NULL,
    email      TEXT,
    photo_uri  TEXT,
    latitude   REAL,
    longitude  REAL,
    favori     INTEGER NOT NULL DEFAULT 0
);
```

## Préférences
| Clé | Composant | Valeurs | Défaut | Effet |
|---|---|---|---|---|
| `taille_police` | ListPreference | 14 / 16 / 20 sp | 16 | nom, prénom, téléphone (liste + détail) |
| `couleur_preferee` | ListPreference | Noir, Bleu, Vert, Rouge, Violet | Noir | nom et prénom (liste + détail) |
| `appel_depuis_liste` | SwitchPreferenceCompat | booléen | false | bouton Appeler (ACTION_DIAL) sur chaque ligne |
| `ordre_tri` | ListPreference | Favoris d'abord / Nom / Prénom | Favoris d'abord | tri de la liste (clause ORDER BY) (Bonus) |

## Bonus (Facultatif)
- **Migration V1 -> V2 sans perte de données** : passage à `DATABASE_VERSION = 2` avec `ALTER TABLE contacts ADD COLUMN favori INTEGER NOT NULL DEFAULT 0` dans `onUpgrade()`.
- **Bascule Favori interactive** : icône étoile sur chaque ligne et case à cocher dans la fiche détaillée.
- **Ordre de tri dynamique** : choix entre "Favoris d'abord", "Nom puis Prénom" ou "Prénom puis Nom" directement appliqué dans les requêtes SQLite via la clause `ORDER BY`.
- **Benchmark d'importation de 100 contacts** : accessible depuis le menu de l'application (option "Benchmark 100 contacts") pour mesurer et comparer la durée avec vs sans transaction unique.

Le compte rendu détaillé (choix techniques, tests, réponses aux questions) est dans [`Compte_Rendu_TP_Contacts_V4.md`](Compte_Rendu_TP_Contacts_V4.md).
