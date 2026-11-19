package com.example.tp_listview.data

/**
 * Règles de validation d'un contact, sans dépendance Android (testables en tests unitaires).
 */
object ContactValidator {

    const val LAT_MIN = -90.0
    const val LAT_MAX = 90.0
    const val LON_MIN = -180.0
    const val LON_MAX = 180.0

    /** Résultat de l'analyse des deux champs GPS saisis. */
    sealed class ResultatGps {
        /** Les deux champs sont vides : position absente (NULL en base). */
        object Absent : ResultatGps()

        /** Coordonnées valides. */
        data class Valide(val latitude: Double, val longitude: Double) : ResultatGps()

        /** Saisie incorrecte : indique quel(s) champ(s) est/sont en erreur. */
        data class Invalide(val latitudeEnErreur: Boolean, val longitudeEnErreur: Boolean) : ResultatGps()
    }

    fun coordonneesValides(latitude: Double?, longitude: Double?): Boolean =
        latitude != null && longitude != null &&
                latitude in LAT_MIN..LAT_MAX && longitude in LON_MIN..LON_MAX

    /**
     * Analyse la saisie texte des coordonnées. Accepte la virgule comme séparateur décimal.
     * Une seule des deux valeurs renseignée est considérée comme invalide.
     */
    fun analyserGps(latSaisie: String, lonSaisie: String): ResultatGps {
        val latStr = latSaisie.trim().replace(',', '.')
        val lonStr = lonSaisie.trim().replace(',', '.')
        if (latStr.isEmpty() && lonStr.isEmpty()) return ResultatGps.Absent

        val lat = latStr.toDoubleOrNull()?.takeIf { it.isFinite() && it in LAT_MIN..LAT_MAX }
        val lon = lonStr.toDoubleOrNull()?.takeIf { it.isFinite() && it in LON_MIN..LON_MAX }

        return if (lat != null && lon != null) ResultatGps.Valide(lat, lon)
        else ResultatGps.Invalide(latitudeEnErreur = lat == null, longitudeEnErreur = lon == null)
    }

    /** Vrai si les trois champs obligatoires (nom, prénom, téléphone) sont renseignés. */
    fun champsObligatoiresRemplis(nom: String, prenom: String, telephone: String): Boolean =
        nom.isNotBlank() && prenom.isNotBlank() && telephone.isNotBlank()
}
