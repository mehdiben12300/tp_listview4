package com.example.tp_listview.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Gestion du fichier contacts.db et de son cycle de vie (création / montée de version).
 *
 * - onCreate n'est appelé que si contacts.db n'existe pas encore : les contacts de
 *   démonstration ne sont donc insérés qu'une seule fois (premier lancement).
 * - Instance unique (singleton) construite avec applicationContext pour éviter les
 *   fuites d'activité et les connexions concurrentes multiples sur le même fichier.
 */
class ContactDbHelper private constructor(
    private val appContext: Context,
    databaseName: String?
) : SQLiteOpenHelper(appContext, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 1
        private const val TAG = "ContactDbHelper"

        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext, DATABASE_NAME)
                    .also { instance = it }
            }

        /** Helper sur une base en mémoire (nom null), réservé aux tests instrumentés. */
        fun creerPourTests(context: Context): ContactDbHelper =
            ContactDbHelper(context.applicationContext, null)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ContactContract.SQL_CREATE_TABLE)
        insererContactsDemo(db)
    }

    /**
     * Version 1 : aucune migration n'existe encore. En cas de changement de schéma,
     * incrémenter DATABASE_VERSION et ajouter ici une étape « if (oldVersion < N) »
     * (ALTER TABLE ...) pour conserver les données. Par défaut, on recrée la table.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Mise à jour de la base $oldVersion -> $newVersion : recréation de la table")
        db.execSQL(ContactContract.SQL_DROP_TABLE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    private fun insererContactsDemo(db: SQLiteDatabase) {
        val pkg = appContext.packageName
        fun avatar(nom: String) = "android.resource://$pkg/drawable/$nom"

        val demo = listOf(
            Contact(nom = "Dupont", prenom = "Claire", telephone = "06 12 34 56 78",
                email = "claire.dupont@example.com", photoUri = avatar("avatar_claire"),
                latitude = 48.8584, longitude = 2.2945),
            Contact(nom = "Martin", prenom = "Thomas", telephone = "07 23 45 67 89",
                email = "thomas.martin@example.com", photoUri = avatar("avatar_thomas"),
                latitude = 45.7578, longitude = 4.8320),
            Contact(nom = "Bernard", prenom = "Sophie", telephone = "06 98 76 54 32",
                email = "sophie.bernard@example.com", photoUri = avatar("avatar_sophie"),
                latitude = 43.2951, longitude = 5.3744),
            // Contact sans coordonnées GPS (1/2)
            Contact(nom = "Petit", prenom = "Lucas", telephone = "07 11 22 33 44",
                email = "lucas.petit@example.com", photoUri = avatar("avatar_lucas")),
            Contact(nom = "Ben Salah", prenom = "Youssef", telephone = "+216 98 765 432",
                email = "youssef.bensalah@example.com", photoUri = avatar("avatar_julien"),
                latitude = 36.8065, longitude = 10.1815),
            // Contact sans coordonnées GPS (2/2), nom avec apostrophe
            Contact(nom = "O'Neil", prenom = "Sarah", telephone = "06 55 44 33 22",
                email = null, photoUri = null),
            Contact(nom = "Trabelsi", prenom = "Amira", telephone = "+216 22 333 444",
                email = "amira.trabelsi@example.com", photoUri = null,
                latitude = 36.8625, longitude = 10.1956)
        )

        db.beginTransaction()
        try {
            demo.forEach { db.insertOrThrow(ContactContract.TABLE_NAME, null, ContactDao.toContentValues(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
