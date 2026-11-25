package com.example.tp_listview.data

import android.provider.BaseColumns

/**
 * Contrat de la base : nom de la table et noms des colonnes regroupés en constantes.
 * Aucun nom de table ou de colonne n'est écrit en dur ailleurs dans le projet.
 */
object ContactContract {

    const val TABLE_NAME = "contacts"

    object Colonnes {
        const val ID = BaseColumns._ID          // "_id"
        const val NOM = "nom"
        const val PRENOM = "prenom"
        const val TELEPHONE = "telephone"
        const val EMAIL = "email"
        const val PHOTO_URI = "photo_uri"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val FAVORI = "favori"             // V2 (Bonus) : 0 = faux, 1 = vrai
    }

    /** Toutes les colonnes, dans l'ordre de la table (projection des requêtes SELECT). */
    val PROJECTION = arrayOf(
        Colonnes.ID, Colonnes.NOM, Colonnes.PRENOM, Colonnes.TELEPHONE,
        Colonnes.EMAIL, Colonnes.PHOTO_URI, Colonnes.LATITUDE, Colonnes.LONGITUDE,
        Colonnes.FAVORI
    )

    /** Tri par nom puis prénom, insensible à la casse. */
    const val ORDRE_PAR_DEFAUT =
        "${Colonnes.NOM} COLLATE NOCASE ASC, ${Colonnes.PRENOM} COLLATE NOCASE ASC"

    /** Tri avec les favoris en tête de liste (Bonus). */
    const val ORDRE_FAVORIS_D_ABORD =
        "${Colonnes.FAVORI} DESC, ${Colonnes.NOM} COLLATE NOCASE ASC, ${Colonnes.PRENOM} COLLATE NOCASE ASC"

    const val SQL_CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            ${Colonnes.ID}        INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Colonnes.NOM}       TEXT NOT NULL,
            ${Colonnes.PRENOM}    TEXT NOT NULL,
            ${Colonnes.TELEPHONE} TEXT NOT NULL,
            ${Colonnes.EMAIL}     TEXT,
            ${Colonnes.PHOTO_URI} TEXT,
            ${Colonnes.LATITUDE}  REAL,
            ${Colonnes.LONGITUDE} REAL,
            ${Colonnes.FAVORI}    INTEGER NOT NULL DEFAULT 0
        )
    """

    /** Migration V1 -> V2 : ajout de la colonne favori sans perte de données */
    const val SQL_UPGRADE_V1_TO_V2 =
        "ALTER TABLE $TABLE_NAME ADD COLUMN ${Colonnes.FAVORI} INTEGER NOT NULL DEFAULT 0"

    const val SQL_DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
}
