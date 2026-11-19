package com.example.tp_listview

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tp_listview.data.Contact
import com.example.tp_listview.data.ContactDao
import com.example.tp_listview.data.ContactDbHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentés du DAO sur une base SQLite en mémoire (n'affecte pas contacts.db).
 * Lancer avec : ./gradlew connectedAndroidTest (émulateur ou appareil requis).
 */
@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var helper: ContactDbHelper
    private lateinit var dao: ContactDao

    @Before fun setUp() {
        helper = ContactDbHelper.creerPourTests(InstrumentationRegistry.getInstrumentation().targetContext)
        dao = ContactDao(helper)
    }

    @After fun tearDown() = helper.close()

    @Test fun contactsDemoInseresUneSeuleFois() {
        val demo = dao.getAll()
        assertTrue(demo.size >= 5)
        assertTrue(demo.count { it.latitude == null && it.longitude == null } >= 2)
    }

    @Test fun listeTrieeParNomPuisPrenomSansCasse() {
        dao.insert(Contact(nom = "aaa", prenom = "Zoé", telephone = "1"))
        dao.insert(Contact(nom = "AAA", prenom = "alice", telephone = "2"))
        val noms = dao.getAll().map { it.nom.lowercase() + " " + it.prenom.lowercase() }
        assertEquals(noms.sorted(), noms)
    }

    @Test fun crudComplet() {
        val id = dao.insert(Contact(nom = "L'Ariana", prenom = "Test", telephone = "+216 70 000 000"))
        assertTrue(id > 0)

        val lu = dao.getById(id)
        assertNotNull(lu)
        assertEquals("L'Ariana", lu!!.nom)
        assertNull(lu.latitude)

        assertEquals(1, dao.update(lu.copy(prenom = "Modifié", latitude = 36.86, longitude = 10.19)))
        assertEquals("Modifié", dao.getById(id)!!.prenom)
        assertEquals(36.86, dao.getById(id)!!.latitude!!, 1e-9)

        assertEquals(1, dao.delete(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.delete(id))
        assertEquals(0, dao.update(lu))
    }

    @Test fun rechercheAvecApostropheEtCaracteresSpeciaux() {
        dao.insert(Contact(nom = "O'Neil", prenom = "Kate", telephone = "06 00 00 00 01"))
        assertTrue(dao.search("o'neil").any { it.nom == "O'Neil" })
        assertTrue(dao.search("'").isNotEmpty())
        assertTrue(dao.search("%").isEmpty())                 // % échappé : pas de joker
        assertTrue(dao.search("0600000001").any { it.prenom == "Kate" }) // espaces ignorés
        assertTrue(dao.search("'; DROP TABLE contacts; --").isEmpty())
        assertTrue(dao.getAll().isNotEmpty())                 // la table existe toujours
    }
}
