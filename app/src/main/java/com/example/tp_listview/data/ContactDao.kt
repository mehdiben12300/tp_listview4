package com.example.tp_listview.data

import android.content.ContentValues
import android.database.Cursor
import com.example.tp_listview.data.ContactContract.Colonnes

/**
 * Seule classe du projet contenant du SQL.
 *
 * - Toutes les valeurs variables passent par des arguments « ? » (selectionArgs).
 * - Chaque Cursor est fermé avec use { } ; les index sont obtenus par getColumnIndexOrThrow().
 * - Les méthodes sont bloquantes : elles doivent être appelées hors du thread principal
 *   (Dispatchers.IO), ce que fait ContactViewModel.
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l'_id de la nouvelle ligne, ou -1 en cas d'échec. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(ContactContract.TABLE_NAME, null, toContentValues(contact))

    /** @return le nombre de lignes modifiées (0 si le contact n'existe plus). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            ContactContract.TABLE_NAME,
            toContentValues(contact),
            "${Colonnes.ID} = ?",
            arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées (0 si le contact n'existe pas). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(
            ContactContract.TABLE_NAME,
            "${Colonnes.ID} = ?",
            arrayOf(id.toString())
        )

    fun getAll(ordre: String = ContactContract.ORDRE_PAR_DEFAUT): List<Contact> =
        helper.readableDatabase.query(
            ContactContract.TABLE_NAME,
            ContactContract.PROJECTION,
            null, null, null, null,
            ordre
        ).use { it.toContacts() }

    fun getById(id: Long): Contact? =
        helper.readableDatabase.query(
            ContactContract.TABLE_NAME,
            ContactContract.PROJECTION,
            "${Colonnes.ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toContact() else null }

    /**
     * Recherche insensible à la casse (LIKE) sur le nom, le prénom, « nom prénom »
     * et le téléphone (espaces ignorés). Les caractères spéciaux de LIKE (%, _) saisis
     * par l'utilisateur sont échappés ; l'apostrophe ne pose aucun problème grâce aux « ? ».
     */
    fun search(texte: String, ordre: String = ContactContract.ORDRE_PAR_DEFAUT): List<Contact> {
        val saisie = texte.trim()
        if (saisie.isEmpty()) return getAll(ordre)

        val motif = "%${echapperLike(saisie)}%"
        val motifTel = "%${echapperLike(saisie.replace(" ", ""))}%"

        val selection = """
            ${Colonnes.NOM} LIKE ? ESCAPE '\'
            OR ${Colonnes.PRENOM} LIKE ? ESCAPE '\'
            OR (${Colonnes.NOM} || ' ' || ${Colonnes.PRENOM}) LIKE ? ESCAPE '\'
            OR (${Colonnes.PRENOM} || ' ' || ${Colonnes.NOM}) LIKE ? ESCAPE '\'
            OR REPLACE(${Colonnes.TELEPHONE}, ' ', '') LIKE ? ESCAPE '\'
        """.trimIndent()

        return helper.readableDatabase.query(
            ContactContract.TABLE_NAME,
            ContactContract.PROJECTION,
            selection,
            arrayOf(motif, motif, motif, motif, motifTel),
            null, null,
            ordre
        ).use { it.toContacts() }
    }

    /**
     * Bascule l'état favori d'un contact (Bonus).
     * @return le nombre de lignes mises à jour (1 si succès).
     */
    fun toggleFavori(id: Long, favori: Boolean): Int {
        val values = ContentValues().apply {
            put(Colonnes.FAVORI, if (favori) 1 else 0)
        }
        return helper.writableDatabase.update(
            ContactContract.TABLE_NAME,
            values,
            "${Colonnes.ID} = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * Importation en masse de contacts avec ou sans transaction SQLite (Bonus).
     * @return la durée écoulée en millisecondes.
     */
    fun importerContacts(contacts: List<Contact>, avecTransaction: Boolean): Long {
        val debut = System.currentTimeMillis()
        val db = helper.writableDatabase
        if (avecTransaction) {
            db.beginTransaction()
            try {
                contacts.forEach { contact ->
                    db.insert(ContactContract.TABLE_NAME, null, toContentValues(contact))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } else {
            contacts.forEach { contact ->
                db.insert(ContactContract.TABLE_NAME, null, toContentValues(contact))
            }
        }
        return System.currentTimeMillis() - debut
    }

    data class ResultatBenchmark(
        val dureeSansMs: Long,
        val dureeAvecMs: Long,
        val nbContacts: Int = 100
    )

    /**
     * Génère 100 contacts et compare l'insertion avec et sans transaction unique (Bonus).
     */
    fun executerBenchmark100Contacts(): ResultatBenchmark {
        val contactsSans = (1..100).map { i ->
            Contact(nom = "BenchSans", prenom = "Contact $i", telephone = "060000%04d".format(i))
        }
        val contactsAvec = (1..100).map { i ->
            Contact(nom = "BenchAvec", prenom = "Contact $i", telephone = "070000%04d".format(i))
        }

        val dureeSans = importerContacts(contactsSans, avecTransaction = false)
        val dureeAvec = importerContacts(contactsAvec, avecTransaction = true)

        // Nettoyage immédiat des contacts de test
        helper.writableDatabase.delete(
            ContactContract.TABLE_NAME,
            "${Colonnes.NOM} IN (?, ?)",
            arrayOf("BenchSans", "BenchAvec")
        )

        return ResultatBenchmark(dureeSans, dureeAvec, 100)
    }

    private fun echapperLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun Cursor.toContacts(): List<Contact> {
        val resultat = ArrayList<Contact>(count)
        while (moveToNext()) resultat.add(toContact())
        return resultat
    }

    private fun Cursor.toContact(): Contact {
        val iId = getColumnIndexOrThrow(Colonnes.ID)
        val iNom = getColumnIndexOrThrow(Colonnes.NOM)
        val iPrenom = getColumnIndexOrThrow(Colonnes.PRENOM)
        val iTel = getColumnIndexOrThrow(Colonnes.TELEPHONE)
        val iEmail = getColumnIndexOrThrow(Colonnes.EMAIL)
        val iPhoto = getColumnIndexOrThrow(Colonnes.PHOTO_URI)
        val iLat = getColumnIndexOrThrow(Colonnes.LATITUDE)
        val iLon = getColumnIndexOrThrow(Colonnes.LONGITUDE)
        val iFavori = getColumnIndex(Colonnes.FAVORI)
        val favori = if (iFavori >= 0 && !isNull(iFavori)) getInt(iFavori) == 1 else false
        return Contact(
            id = getLong(iId),
            nom = getString(iNom),
            prenom = getString(iPrenom),
            telephone = getString(iTel),
            email = if (isNull(iEmail)) null else getString(iEmail),
            photoUri = if (isNull(iPhoto)) null else getString(iPhoto),
            latitude = if (isNull(iLat)) null else getDouble(iLat),
            longitude = if (isNull(iLon)) null else getDouble(iLon),
            favori = favori
        )
    }

    companion object {
        /** Conversion Contact -> ContentValues (l'_id n'est jamais écrit : AUTOINCREMENT). */
        fun toContentValues(contact: Contact): ContentValues = ContentValues().apply {
            put(Colonnes.NOM, contact.nom)
            put(Colonnes.PRENOM, contact.prenom)
            put(Colonnes.TELEPHONE, contact.telephone)
            if (contact.email.isNullOrBlank()) putNull(Colonnes.EMAIL) else put(Colonnes.EMAIL, contact.email)
            if (contact.photoUri.isNullOrBlank()) putNull(Colonnes.PHOTO_URI) else put(Colonnes.PHOTO_URI, contact.photoUri)
            if (contact.latitude == null) putNull(Colonnes.LATITUDE) else put(Colonnes.LATITUDE, contact.latitude)
            if (contact.longitude == null) putNull(Colonnes.LONGITUDE) else put(Colonnes.LONGITUDE, contact.longitude)
            put(Colonnes.FAVORI, if (contact.favori) 1 else 0)
        }
    }
}
