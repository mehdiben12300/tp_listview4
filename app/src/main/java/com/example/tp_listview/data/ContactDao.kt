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

    fun getAll(): List<Contact> =
        helper.readableDatabase.query(
            ContactContract.TABLE_NAME,
            ContactContract.PROJECTION,
            null, null, null, null,
            ContactContract.ORDRE_PAR_DEFAUT
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
    fun search(texte: String): List<Contact> {
        val saisie = texte.trim()
        if (saisie.isEmpty()) return getAll()

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
            ContactContract.ORDRE_PAR_DEFAUT
        ).use { it.toContacts() }
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
        return Contact(
            id = getLong(iId),
            nom = getString(iNom),
            prenom = getString(iPrenom),
            telephone = getString(iTel),
            email = if (isNull(iEmail)) null else getString(iEmail),
            photoUri = if (isNull(iPhoto)) null else getString(iPhoto),
            latitude = if (isNull(iLat)) null else getDouble(iLat),
            longitude = if (isNull(iLon)) null else getDouble(iLon)
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
        }
    }
}
