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
    }

    /** Toutes les colonnes, dans l'ordre de la table (projection des requêtes SELECT). */
    val PROJECTION = arrayOf(
        Colonnes.ID, Colonnes.NOM, Colonnes.PRENOM, Colonnes.TELEPHONE,
        Colonnes.EMAIL, Colonnes.PHOTO_URI, Colonnes.LATITUDE, Colonnes.LONGITUDE
    )

    /** Tri par nom puis prénom, insensible à la casse. */
    const val ORDRE_PAR_DEFAUT =
        "${Colonnes.NOM} COLLATE NOCASE ASC, ${Colonnes.PRENOM} COLLATE NOCASE ASC"

    const val SQL_CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            ${Colonnes.ID}        INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Colonnes.NOM}       TEXT NOT NULL,
            ${Colonnes.PRENOM}    TEXT NOT NULL,
            ${Colonnes.TELEPHONE} TEXT NOT NULL,
            ${Colonnes.EMAIL}     TEXT,
            ${Colonnes.PHOTO_URI} TEXT,
            ${Colonnes.LATITUDE}  REAL,
            ${Colonnes.LONGITUDE} REAL
        )
    """

    const val SQL_DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
}
