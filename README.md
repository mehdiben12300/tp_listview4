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

## Schéma
```sql
CREATE TABLE contacts (
    _id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nom        TEXT NOT NULL,
    prenom     TEXT NOT NULL,
    telephone  TEXT NOT NULL,
    email      TEXT,
    photo_uri  TEXT,
    latitude   REAL,
    longitude  REAL
);
```

## Préférences
| Clé | Composant | Valeurs | Défaut | Effet |
|---|---|---|---|---|
| `taille_police` | ListPreference | 14 / 16 / 20 sp | 16 | nom, prénom, téléphone (liste + détail) |
| `couleur_preferee` | ListPreference | Noir, Bleu, Vert, Rouge, Violet | Noir | nom et prénom (liste + détail) |
| `appel_depuis_liste` | SwitchPreferenceCompat | booléen | false | bouton Appeler (ACTION_DIAL) sur chaque ligne |

Le compte rendu détaillé (choix techniques, tests, réponses aux questions) est dans [`Compte_Rendu_TP_Contacts_V4.md`](Compte_Rendu_TP_Contacts_V4.md).
