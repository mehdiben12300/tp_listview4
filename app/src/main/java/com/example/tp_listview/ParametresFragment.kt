package com.example.tp_listview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar

/**
 * Écran Paramètres : trois préférences décrites dans res/xml/preferences.xml,
 * stockées automatiquement dans les SharedPreferences par défaut.
 * Le résumé de chaque préférence affiche la valeur choisie (useSimpleSummaryProvider).
 */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    /** Ajoute une barre d'outils (titre + retour) au-dessus de la liste des préférences. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val racine = inflater.inflate(R.layout.fragment_parametres, container, false) as ViewGroup
        val conteneur = racine.findViewById<FrameLayout>(R.id.parametres_container)
        val listePreferences = super.onCreateView(inflater, conteneur, savedInstanceState)
        conteneur.addView(listePreferences)
        racine.findViewById<MaterialToolbar>(R.id.toolbarParametres).setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        return racine
    }
}
