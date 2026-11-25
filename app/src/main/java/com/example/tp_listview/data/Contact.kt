package com.example.tp_listview.data

/**
 * Modèle Contact (V4).
 *
 * [id] vaut 0 tant que le contact n'est pas enregistré en base, puis prend la valeur
 * de la colonne _id attribuée par SQLite lors de l'insertion.
 * [photoUri] contient soit le chemin absolu d'une copie de l'image dans filesDir,
 * soit une URI android.resource:// pour les avatars des contacts de démonstration.
 */
data class Contact(
    val id: Long = 0L,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val favori: Boolean = false
) {

    val nomComplet: String
        get() = "$nom $prenom".trim()

    val estNouveau: Boolean
        get() = id == 0L

    /** Latitude dans [-90 ; 90] et longitude dans [-180 ; 180], toutes deux renseignées. */
    val hasValidCoordinates: Boolean
        get() = ContactValidator.coordonneesValides(latitude, longitude)

    fun formattedCoordinates(): String =
        if (hasValidCoordinates) "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
        else "Aucune position définie"
}
