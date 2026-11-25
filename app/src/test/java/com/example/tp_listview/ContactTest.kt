package com.example.tp_listview

import com.example.tp_listview.data.Contact
import com.example.tp_listview.data.ContactValidator
import com.example.tp_listview.data.ContactValidator.ResultatGps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests unitaires (JVM) : modèle Contact, validation GPS / champs, conversion des préférences. */
class ContactTest {

    private fun contact(lat: Double? = null, lon: Double? = null) =
        Contact(nom = "Dupont", prenom = "Claire", telephone = "06 12 34 56 78", latitude = lat, longitude = lon)

    @Test fun nomComplet() = assertEquals("Dupont Claire", contact().nomComplet)

    @Test fun nouveauContactAIdZero() {
        assertEquals(0L, contact().id)
        assertTrue(contact().estNouveau)
        assertFalse(contact().copy(id = 12L).estNouveau)
    }

    @Test fun coordonneesValides() = assertTrue(contact(48.8566, 2.3522).hasValidCoordinates)
    @Test fun coordonneesNulles() = assertFalse(contact().hasValidCoordinates)
    @Test fun latitudeHorsLimites() = assertFalse(contact(95.0, 2.0).hasValidCoordinates)
    @Test fun longitudeHorsLimites() = assertFalse(contact(45.0, 195.0).hasValidCoordinates)
    @Test fun limitesExactes() = assertTrue(contact(90.0, -180.0).hasValidCoordinates)

    @Test fun gpsAbsent() = assertEquals(ResultatGps.Absent, ContactValidator.analyserGps("", "  "))

    @Test fun gpsValideAvecVirgule() =
        assertEquals(ResultatGps.Valide(36.8065, 10.1815), ContactValidator.analyserGps("36,8065", "10.1815"))

    @Test fun gpsNonNumerique() =
        assertEquals(ResultatGps.Invalide(true, false), ContactValidator.analyserGps("abc", "10"))

    @Test fun gpsHorsLimites() =
        assertEquals(ResultatGps.Invalide(false, true), ContactValidator.analyserGps("10", "200"))

    @Test fun gpsUneSeuleValeur() =
        assertEquals(ResultatGps.Invalide(false, true), ContactValidator.analyserGps("10", ""))

    @Test fun gpsNaNRefuse() =
        assertEquals(ResultatGps.Invalide(true, false), ContactValidator.analyserGps("NaN", "10"))

    @Test fun champsObligatoires() {
        assertTrue(ContactValidator.champsObligatoiresRemplis("O'Neil", "Sarah", "+216 22 333 444"))
        assertFalse(ContactValidator.champsObligatoiresRemplis("", "Sarah", "06"))
        assertFalse(ContactValidator.champsObligatoiresRemplis("Nom", " ", "06"))
        assertFalse(ContactValidator.champsObligatoiresRemplis("Nom", "Prénom", ""))
    }

    @Test fun conversionTaillePolice() {
        assertEquals(14f, PreferencesManager.convertirTaille("14"))
        assertEquals(20f, PreferencesManager.convertirTaille("20"))
        assertEquals(16f, PreferencesManager.convertirTaille(null))
        assertEquals(16f, PreferencesManager.convertirTaille("abc"))
        assertEquals(16f, PreferencesManager.convertirTaille("99"))
    }

    @Test fun contactFavoriParDefautFaux() {
        assertFalse(contact().favori)
        assertTrue(contact().copy(favori = true).favori)
    }
}
