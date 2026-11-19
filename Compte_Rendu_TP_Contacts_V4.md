# Compte rendu — TP Android Contacts V4
**Persistance SQLite • Préférences utilisateur • Fragments • Google Maps**

## 1. Schéma de la base et choix des types
Base `contacts.db`, table `contacts` (voir `ContactContract.SQL_CREATE_TABLE`) :

| Colonne | Type | Contrainte | Justification |
|---|---|---|---|
| `_id` | INTEGER | PRIMARY KEY AUTOINCREMENT | identifiant stable, utilisé partout (Bundle, adapter, ViewModel) ; nom `_id` = `BaseColumns._ID` |
| `nom`, `prenom` | TEXT | NOT NULL | champs obligatoires |
| `telephone` | TEXT | NOT NULL | un numéro peut commencer par `0` ou `+216` et contenir des espaces : un INTEGER perdrait ces informations |
| `email` | TEXT | — | facultatif (NULL si vide) |
| `photo_uri` | TEXT | — | chemin absolu de la copie dans `filesDir/photos` (ou URI `android.resource://` pour les avatars de démonstration) |
| `latitude`, `longitude` | REAL | NULL autorisé | position absente = NULL dans les deux colonnes |

Tri : `nom COLLATE NOCASE ASC, prenom COLLATE NOCASE ASC` (constante `ORDRE_PAR_DEFAUT`).

## 2. Rôle du helper et du DAO
- **`ContactDbHelper`** (`SQLiteOpenHelper`) : crée la table et insère 7 contacts de démonstration dans une transaction (dont 2 sans GPS : *Petit Lucas* et *O'Neil Sarah*). `DATABASE_VERSION = 1`, `onUpgrade` recrée la table (point d'extension documenté pour une future migration `ALTER TABLE`). Singleton `getInstance(context)` construit avec `applicationContext` (double-checked locking + `@Volatile`).
- **`ContactDao`** : seule classe contenant du SQL. `insert` → `Long` (_id ou -1), `update`/`delete` → `Int` (nombre de lignes), `getAll`, `getById`, `search`. Toutes les valeurs variables passent par des `?` ; chaque `Cursor` est fermé par `use {}` ; les index sont lus par `getColumnIndexOrThrow()`.
- **`ContactContract`** : nom de table et colonnes en constantes, aucun nom de colonne en dur ailleurs.

## 3. Stratégie de threads
- `ContactViewModel` (AndroidViewModel partagé, portée activité) exécute tous les appels DAO avec `withContext(Dispatchers.IO)` dans `viewModelScope`.
- Les écritures (`enregistrer`, `supprimer`) sont lancées via `viewModelScope.async(Dispatchers.IO)` : elles se terminent même si le fragment est détruit (rotation) ; le fragment attend le résultat dans `viewLifecycleOwner.lifecycleScope`.
- Le rechargement de la liste annule la requête précédente (`Job.cancel()`) pour éviter qu'un résultat obsolète écrase un plus récent pendant la saisie du filtre.
- La copie des photos (galerie/caméra) se fait aussi sur `Dispatchers.IO`.

## 4. Choix du filtrage : SQL
Le filtrage est réalisé **en SQL** (`ContactDao.search`) :
- `LIKE ? ESCAPE '\'` sur `nom`, `prenom`, `nom || ' ' || prenom`, `prenom || ' ' || nom` et `REPLACE(telephone, ' ', '')` (les espaces du téléphone sont ignorés).
- `LIKE` est insensible à la casse pour les caractères ASCII ; le tri reste `COLLATE NOCASE`.
- Les caractères `%` et `_` saisis sont échappés : ils sont recherchés littéralement. L'apostrophe (« O'Neil ») ne pose aucun problème puisqu'elle n'est jamais concaténée.

**Justification** : la base reste la source de vérité unique (la liste affichée = résultat d'une requête), cela démontre les requêtes paramétrées et le comportement se maintient si le nombre de contacts augmente. Le coût (une requête par frappe) est négligeable sur un petit volume et le `Job` précédent est annulé. *Limite* : `LIKE` ne gère pas la casse des lettres accentuées (É/é) ; un filtrage en mémoire avec `lowercase()` l'aurait géré.

## 5. Gestion des photos
- Galerie (`GetContent`) : l'image est décodée avec sous-échantillonnage (≤ ~512 px) puis **copiée** en JPEG dans `filesDir/photos/contact_<timestamp>.jpg`. Le chemin absolu est enregistré dans `photo_uri`. Une URI `content://` de galerie n'est pas conservée car elle peut devenir illisible après redémarrage.
- Caméra (`TakePicturePreview`) : le Bitmap est enregistré de la même façon.
- Nettoyage : les copies faites pendant une saisie abandonnée (Annuler / Retour) sont supprimées ; l'ancienne photo est supprimée quand elle est remplacée ou que le contact est supprimé.
- Sauvegardé dans `onSaveInstanceState` : la photo en cours de choix survit à la rotation.

## 6. Préférences
- **Stockage** : `SharedPreferences` par défaut (géré automatiquement par `PreferenceFragmentCompat`), pas dans SQLite. Valeurs par défaut écrites au premier lancement (`PreferenceManager.setDefaultValues`).
- **Écran** : `ParametresFragment` (`res/xml/preferences.xml`, tableaux dans `res/values/arrays.xml`), ouvert par l'icône ⚙ de la barre d'application avec `replace()` + `addToBackStack()`. Les ListPreference utilisent `useSimpleSummaryProvider`; le switch affiche `summaryOn`/`summaryOff`.
- **Lecture centralisée** : `PreferencesManager.lireAffichage()` renvoie `Affichage(tailleSp: Float, couleurTexte: Int, appelDepuisListe: Boolean)` déjà converties (valeurs invalides → défaut).
- **Application immédiate** : `ListeContactsFragment.onResume()` relit les préférences et appelle `adapter.appliquerAffichage()`, qui ne redessine les lignes que si les valeurs ont changé. `DetailContactFragment` applique taille/couleur à la création de sa vue.
- **Couleurs** : teintes foncées (#000000, #1565C0, #2E7D32, #C62828, #6A1B9A), lisibles sur le fond blanc. L'application utilise un thème clair (`Theme.Material3.Light`) y compris quand le système est en mode sombre, pour garantir ce contraste.
- **Bouton Appeler dans la liste** : `Intent.ACTION_DIAL` ; le bouton a son propre listener (le clic n'ouvre pas le détail) ; numéro vide → message ; absence d'application téléphone → `ActivityNotFoundException` interceptée.

## 7. Robustesse
- Validation : nom, prénom, téléphone obligatoires ; e-mail vérifié s'il est saisi ; GPS : les deux champs ou aucun, numériques, latitude ∈ [-90 ; 90], longitude ∈ [-180 ; 180] (virgule acceptée). En cas d'erreur, rien n'est écrit en base.
- Résultats exploités : `insert = -1` → « L'insertion a échoué » ; `update/delete = 0` → « Contact introuvable » ; `SQLiteException` → message d'erreur.
- Rotation : fragment initial ajouté uniquement si `savedInstanceState == null` ; le détail ne recharge la base qu'à sa première création, la saisie est restaurée par les `EditText`.
- Bouton Enregistrer désactivé pendant l'écriture (pas de double insertion).
- Carte : ouverte même sans position valide, avec un message explicite (absente ou invalide).

## 8. Tests réalisés
**Automatisés**
- `ContactTest` (JVM, 15 tests, tous verts) : nom complet, id = 0 pour un nouveau contact, limites GPS, saisies vides / non numériques / hors limites / NaN, champs obligatoires, conversion de la taille de police.
- `ContactDaoTest` (instrumenté, base en mémoire) : contacts démo (≥ 5, ≥ 2 sans GPS), tri insensible à la casse, CRUD complet avec valeurs de retour, recherche avec apostrophe, `%`, tentative d'injection `'; DROP TABLE contacts; --`.

**Manuels (à réaliser sur appareil, avec captures)**
| Test | Résultat attendu |
|---|---|
| 1er lancement / 2e lancement | 7 contacts démo, non dupliqués |
| Ajouter, modifier, supprimer puis fermer l'app (glisser hors des récentes) | modifications conservées |
| Nom « O'Neil » / « L'Ariana », filtre `'` | aucune erreur SQL |
| Enregistrer sans nom/prénom/téléphone | message, rien en base |
| Changer la photo puis redémarrer l'appareil | photo toujours affichée |
| GPS absent / « abc » / 95 | rien d'invalide enregistré, carte avec message |
| Chaque taille et couleur | appliquées dans la liste et le détail |
| Activer/désactiver l'appel | bouton visible/masqué, composeur avec le bon numéro |
| Relance | préférences conservées |
| Rotation sur liste / détail en saisie / paramètres ; Retour | pas de doublon, saisie conservée |
| App Inspection → Database Inspector | contenu de `contacts` identique à l'affichage |

## 9. Difficultés rencontrées
- `onCreate` du helper n'est exécuté que si `contacts.db` n'existe pas : pendant le développement il faut désinstaller l'application (ou incrémenter la version).
- Recharger le contact depuis la base dans `onViewCreated` écrasait la saisie après rotation → chargement uniquement si `savedInstanceState == null`.
- La ligne de la RecyclerView contenait un conteneur `clickable` qui interceptait le clic : supprimé pour que seul le bouton Appeler consomme son propre clic.
- Valeur par défaut `#000000` dans le XML : passée par une ressource `@string` pour qu'aapt ne la compile pas en couleur (ce qui aurait donné `#ff000000`, absent des `entryValues`).

## 10. Réponses aux questions
1. **onCreate / onUpgrade** : `onCreate` est appelé lors de la première ouverture (`getWritableDatabase`/`getReadableDatabase`) si le fichier n'existe pas. `onUpgrade` est appelé si la version stockée dans le fichier est inférieure à `DATABASE_VERSION`. Modifier `onCreate` sans changer la version n'a **aucun effet** sur une base existante : il faut désinstaller/effacer les données ou incrémenter la version et écrire `onUpgrade`.
2. **Arguments `?`** : la valeur est liée séparément du texte SQL, elle ne peut donc jamais être interprétée comme du SQL (pas d'injection, pas de problème d'échappement). Exemple : `"SELECT * FROM contacts WHERE nom = '" + saisie + "'"` avec la saisie `O'Neil` produit `... = 'O'Neil'` → erreur de syntaxe ; `' OR '1'='1` renvoie toute la table.
3. **Valeurs de retour** : `insert()` renvoie l'_id de la nouvelle ligne ou **-1** ; `update()` et `delete()` renvoient le **nombre de lignes affectées**. Dans l'interface : -1 → « L'insertion a échoué » ; 0 → « Contact introuvable » ; sinon message de succès et retour à la liste.
4. **Thread principal** : un accès disque peut bloquer l'UI (saccades, ANR après ~5 s, `StrictMode`). Évité avec `viewModelScope` + `withContext(Dispatchers.IO)` / `async(Dispatchers.IO)`.
5. **Helper unique + applicationContext** : une seule connexion partagée évite les verrous et conflits d'écriture concurrentes (`database is locked`) et le coût d'ouvertures multiples ; `applicationContext` vit aussi longtemps que l'application, alors qu'un contexte d'activité retenu par un singleton provoquerait une fuite mémoire après rotation.
6. **SharedPreferences vs SQLite** : SharedPreferences pour quelques valeurs simples clé/valeur (réglages) sans requêtes ; SQLite pour des données structurées, nombreuses, interrogées (tri, filtre) et modifiées en CRUD. Contacts → SQLite ; réglages d'affichage → SharedPreferences.
7. **Application sans redémarrage** : au retour des Paramètres, `ListeContactsFragment.onResume()` relit les préférences via `PreferencesManager` et appelle `adapter.appliquerAffichage()` qui rafraîchit les lignes (`notifyItemRangeChanged`). Le détail lit les préférences à chaque création de vue.
8. **ACTION_DIAL vs ACTION_CALL** : `ACTION_DIAL` ouvre le composeur avec le numéro pré-rempli, l'utilisateur confirme l'appel : **aucune permission**. `ACTION_CALL` lance l'appel directement et exige la permission dangereuse `CALL_PHONE` (demande à l'exécution).
9. **Désinstallation / effacement des données** : dans les deux cas, `contacts.db`, les SharedPreferences et les photos de `filesDir` (stockage interne privé) sont supprimés ; au lancement suivant, la base est recréée avec les contacts démo et les préférences reprennent leurs valeurs par défaut. (Exception : une sauvegarde automatique Android peut restaurer les données après réinstallation si `allowBackup` est actif.)

## 11. Captures à joindre
Liste au premier lancement, création, modification, confirmation de suppression, liste après relance, Database Inspector (table `contacts`), écran Paramètres, liste avec deux tailles et deux couleurs, liste avec boutons Appeler, composeur ouvert depuis la liste, carte avec marqueur.
