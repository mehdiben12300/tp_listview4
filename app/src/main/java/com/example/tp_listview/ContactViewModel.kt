package com.example.tp_listview

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tp_listview.data.Contact
import com.example.tp_listview.data.ContactDao
import com.example.tp_listview.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel partagé (portée : MainActivity) entre la liste et le détail.
 *
 * - Expose la liste (déjà filtrée et triée par SQLite) via LiveData.
 * - Lance tous les appels au DAO dans viewModelScope + Dispatchers.IO :
 *   aucun accès à la base n'a lieu sur le thread principal.
 * - Les opérations d'écriture tournent dans viewModelScope : elles se terminent même si
 *   le fragment est détruit (rotation, retour) pendant l'enregistrement.
 * - Après chaque modification, la liste est rechargée depuis la base.
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _chargementTermine = MutableLiveData(false)
    val chargementTermine: LiveData<Boolean> = _chargementTermine

    var searchQuery: String = ""
        private set

    private var jobChargement: Job? = null

    init {
        rafraichir()
    }

    fun setSearchQuery(query: String) {
        if (query == searchQuery) return
        searchQuery = query
        rafraichir()
    }

    /** Recharge la liste depuis la base en tenant compte du filtre courant. */
    fun rafraichir() {
        jobChargement?.cancel()  // une nouvelle saisie annule la requête précédente
        val filtre = searchQuery
        jobChargement = viewModelScope.launch {
            val liste = try {
                withContext(Dispatchers.IO) {
                    if (filtre.isBlank()) dao.getAll() else dao.search(filtre)
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "Lecture de la base impossible", e)
                emptyList()
            }
            _contacts.value = liste
            _chargementTermine.value = true
        }
    }

    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try {
            dao.getById(id)
        } catch (e: SQLiteException) {
            Log.e(TAG, "getById($id)", e); null
        }
    }

    /**
     * Insère (id == 0) ou met à jour le contact.
     * @param anciennePhoto photo enregistrée avant modification, supprimée de filesDir si remplacée.
     */
    suspend fun enregistrer(contact: Contact, anciennePhoto: String?): ResultatOperation =
        viewModelScope.async(Dispatchers.IO) {
            val resultat = try {
                if (contact.estNouveau) {
                    val id = dao.insert(contact)
                    if (id > 0) ResultatOperation.Succes(id) else ResultatOperation.EchecInsertion
                } else {
                    val lignes = dao.update(contact)
                    if (lignes > 0) ResultatOperation.Succes(contact.id) else ResultatOperation.Introuvable
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "Enregistrement impossible", e)
                ResultatOperation.Erreur(e.message.orEmpty())
            }
            if (resultat is ResultatOperation.Succes && anciennePhoto != contact.photoUri) {
                supprimerPhotoInterne(anciennePhoto)
            }
            resultat
        }.await().also { rafraichir() }

    suspend fun supprimer(contact: Contact): ResultatOperation =
        viewModelScope.async(Dispatchers.IO) {
            try {
                if (dao.delete(contact.id) > 0) {
                    supprimerPhotoInterne(contact.photoUri)
                    ResultatOperation.Succes(contact.id)
                } else {
                    ResultatOperation.Introuvable
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "Suppression impossible", e)
                ResultatOperation.Erreur(e.message.orEmpty())
            }
        }.await().also { rafraichir() }

    /** Supprime une photo uniquement si elle se trouve dans le stockage interne de l'application. */
    private fun supprimerPhotoInterne(chemin: String?) {
        if (chemin.isNullOrBlank()) return
        val fichier = File(chemin)
        val dossierInterne = getApplication<Application>().filesDir.absolutePath
        if (fichier.absolutePath.startsWith(dossierInterne) && fichier.exists()) {
            fichier.delete()
        }
    }

    sealed class ResultatOperation {
        data class Succes(val id: Long) : ResultatOperation()
        object EchecInsertion : ResultatOperation()
        object Introuvable : ResultatOperation()
        data class Erreur(val message: String) : ResultatOperation()
    }

    private companion object {
        const val TAG = "ContactViewModel"
    }
}
