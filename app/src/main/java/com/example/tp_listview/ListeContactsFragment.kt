package com.example.tp_listview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tp_listview.data.Contact
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Liste des contacts chargée depuis SQLite, filtrage au fil de la saisie,
 * bouton « + » de création et bouton Appeler par ligne selon la préférence.
 */
class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var preferences: PreferencesManager
    private lateinit var adapter: ContactAdapter

    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var rvContacts: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        etSearch = view.findViewById(R.id.etSearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        rvContacts = view.findViewById(R.id.rvContacts)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)

        setupToolbar(view.findViewById(R.id.toolbarListe))
        setupRecyclerView()
        setupSearch()
        view.findViewById<FloatingActionButton>(R.id.fabAjouter).setOnClickListener {
            naviguerVers(DetailContactFragment.newInstance(contactId = null))
        }
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Préférences relues à chaque retour (ex. depuis Paramètres) : application immédiate.
        adapter.appliquerAffichage(preferences.lireAffichage())
        // La liste est rechargée depuis la base à chaque affichage.
        viewModel.rafraichir()
    }

    private fun setupToolbar(toolbar: MaterialToolbar) {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_parametres -> {
                    naviguerVers(ParametresFragment()); true
                }
                R.id.action_benchmark -> {
                    lancerBenchmark(); true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            onContactClick = { id -> naviguerVers(DetailContactFragment.newInstance(id)) },
            onAppelClick = { telephone -> composerNumero(telephone) },
            onFavoriClick = { contact ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.toggleFavori(contact)
                }
            }
        )
        rvContacts.layoutManager = LinearLayoutManager(requireContext())
        rvContacts.adapter = adapter
    }

    private fun lancerBenchmark() {
        Toast.makeText(requireContext(), "Exécution du benchmark en cours...", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val res = viewModel.executerBenchmark100Contacts()
            val ratio = if (res.dureeAvecMs > 0) res.dureeSansMs.toDouble() / res.dureeAvecMs else 1.0
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.dlg_benchmark_titre)
                .setMessage(getString(R.string.dlg_benchmark_msg, res.dureeSansMs, res.dureeAvecMs, ratio))
                .setPositiveButton(R.string.dlg_fermer, null)
                .show()
        }
    }

    private fun setupSearch() {
        if (etSearch.text.toString() != viewModel.searchQuery) {
            etSearch.setText(viewModel.searchQuery)
        }
        btnClearSearch.visibility = if (viewModel.searchQuery.isNotEmpty()) View.VISIBLE else View.GONE

        etSearch.doOnTextChanged { texte, _, _, _ ->
            val query = texte?.toString().orEmpty()
            viewModel.setSearchQuery(query)
            btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
        }
        btnClearSearch.setOnClickListener { etSearch.text?.clear() }
    }

    private fun observeViewModel() {
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
            val vide = contacts.isEmpty() && viewModel.chargementTermine.value == true
            layoutEmptyState.visibility = if (vide) View.VISIBLE else View.GONE
            rvContacts.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
            tvEmptyMessage.setText(
                if (viewModel.searchQuery.isBlank()) R.string.msg_aucun_contact
                else R.string.msg_aucun_contact_recherche
            )
        }
    }

    /** ACTION_DIAL : ouvre le composeur sans appeler, aucune permission requise. */
    private fun composerNumero(telephone: String) {
        if (telephone.isBlank()) {
            Toast.makeText(requireContext(), R.string.err_telephone_vide, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(telephone)}"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.msg_aucune_app_tel, Toast.LENGTH_SHORT).show()
        }
    }

    private fun naviguerVers(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
