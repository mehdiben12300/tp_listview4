package com.example.tp_listview

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager

/**
 * Activité unique hébergeant les fragments dans un FragmentContainerView.
 * La barre d'application de la liste donne accès aux Paramètres.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Écrit les valeurs par défaut de preferences.xml au premier lancement uniquement.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Après une rotation, le FragmentManager restaure lui-même la pile :
        // on n'ajoute le fragment initial qu'à la première création (pas de doublon).
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, ListeContactsFragment())
                .commit()
        }
    }
}
