package com.example.tp_listview

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager

/**
 * Point unique de lecture et de conversion des préférences utilisateur (SharedPreferences).
 * Les fragments et l'adapter reçoivent des valeurs déjà converties : taille en sp (Float),
 * couleur en Int, booléen.
 */
class PreferencesManager(context: Context) {

    companion object {
        const val CLE_TAILLE_POLICE = "taille_police"
        const val CLE_COULEUR_PREFEREE = "couleur_preferee"
        const val CLE_APPEL_DEPUIS_LISTE = "appel_depuis_liste"
        const val CLE_ORDRE_TRI = "ordre_tri"

        const val TAILLE_DEFAUT_SP = 16f
        const val COULEUR_DEFAUT = "#000000"
        const val APPEL_DEFAUT = false

        const val TRI_FAVORIS = "favoris"
        const val TRI_NOM = "nom"
        const val TRI_PRENOM = "prenom"

        private val TAILLES_AUTORISEES = setOf(14f, 16f, 20f)

        /** Conversion texte -> taille en sp, avec repli sur la valeur par défaut. */
        fun convertirTaille(valeur: String?): Float =
            valeur?.toFloatOrNull()?.takeIf { it in TAILLES_AUTORISEES } ?: TAILLE_DEFAUT_SP
    }

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /** Instantané des préférences d'affichage. */
    data class Affichage(
        val tailleSp: Float,
        val couleurTexte: Int,
        val appelDepuisListe: Boolean
    )

    fun tailleSp(): Float = convertirTaille(prefs.getString(CLE_TAILLE_POLICE, null))

    fun couleurTexte(): Int {
        val valeur = prefs.getString(CLE_COULEUR_PREFEREE, COULEUR_DEFAUT) ?: COULEUR_DEFAUT
        return try {
            Color.parseColor(valeur)
        } catch (e: IllegalArgumentException) {
            Color.parseColor(COULEUR_DEFAUT)
        }
    }

    fun appelDepuisListe(): Boolean = prefs.getBoolean(CLE_APPEL_DEPUIS_LISTE, APPEL_DEFAUT)

    fun ordreTri(): String = prefs.getString(CLE_ORDRE_TRI, TRI_FAVORIS) ?: TRI_FAVORIS

    fun ordreTriSql(): String = when (ordreTri()) {
        TRI_NOM -> "${com.example.tp_listview.data.ContactContract.Colonnes.NOM} COLLATE NOCASE ASC, ${com.example.tp_listview.data.ContactContract.Colonnes.PRENOM} COLLATE NOCASE ASC"
        TRI_PRENOM -> "${com.example.tp_listview.data.ContactContract.Colonnes.PRENOM} COLLATE NOCASE ASC, ${com.example.tp_listview.data.ContactContract.Colonnes.NOM} COLLATE NOCASE ASC"
        else -> "${com.example.tp_listview.data.ContactContract.Colonnes.FAVORI} DESC, ${com.example.tp_listview.data.ContactContract.Colonnes.NOM} COLLATE NOCASE ASC, ${com.example.tp_listview.data.ContactContract.Colonnes.PRENOM} COLLATE NOCASE ASC"
    }

    fun lireAffichage(): Affichage = Affichage(tailleSp(), couleurTexte(), appelDepuisListe())
}
