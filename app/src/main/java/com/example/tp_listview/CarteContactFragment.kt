package com.example.tp_listview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

/**
 * 6. Fragment cartographique :
 * - Mode 1 : Carte interactive avec textures réelles (OpenStreetMap / Leaflet), opérationnelle immédiatement sans clé API.
 * - Mode 2 : Google Maps SDK officiel (SupportMapFragment) pour la conformité académique du TP.
 * - Sélecteur permettant de basculer facilement entre les deux modes.
 */
class CarteContactFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val ARG_CONTACT_ID = "arg_contact_id"
        private const val ARG_CONTACT_NOM = "arg_contact_nom"
        private const val ARG_LATITUDE = "arg_latitude"
        private const val ARG_LONGITUDE = "arg_longitude"
        private const val ARG_INVALIDES = "arg_coordonnees_invalides"

        fun newInstance(
            contactId: Long?,
            contactNom: String,
            latitude: Double?,
            longitude: Double?,
            coordonneesInvalides: Boolean = false
        ): CarteContactFragment {
            return CarteContactFragment().apply {
                arguments = Bundle().apply {
                    if (contactId != null) putLong(ARG_CONTACT_ID, contactId)
                    putBoolean(ARG_INVALIDES, coordonneesInvalides)
                    putString(ARG_CONTACT_NOM, contactNom)
                    if (latitude != null) putDouble(ARG_LATITUDE, latitude)
                    if (longitude != null) putDouble(ARG_LONGITUDE, longitude)
                }
            }
        }
    }

    private var contactNom: String = "Contact"
    private var latitude: Double? = null
    private var longitude: Double? = null

    private lateinit var toolbarCarte: MaterialToolbar
    private lateinit var toggleMapType: MaterialButtonToggleGroup
    private lateinit var btnModeInteractive: MaterialButton
    private lateinit var btnModeGoogleSdk: MaterialButton
    private lateinit var webViewMap: WebView
    private lateinit var mapFragmentContainer: View

    private lateinit var cardMapInfo: CardView
    private lateinit var tvMapContactNom: TextView
    private lateinit var tvMapCoordinates: TextView
    private lateinit var btnOpenExternalMap: MaterialButton
    private lateinit var cardApiKeyWarning: CardView
    private lateinit var cardMapWarning: CardView

    private var googleMapInstance: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            contactNom = args.getString(ARG_CONTACT_NOM, "Contact")
            if (args.containsKey(ARG_LATITUDE)) {
                latitude = args.getDouble(ARG_LATITUDE)
            }
            if (args.containsKey(ARG_LONGITUDE)) {
                longitude = args.getDouble(ARG_LONGITUDE)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_carte_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarCarte = view.findViewById(R.id.toolbarCarte)
        toggleMapType = view.findViewById(R.id.toggleMapType)
        btnModeInteractive = view.findViewById(R.id.btnModeInteractive)
        btnModeGoogleSdk = view.findViewById(R.id.btnModeGoogleSdk)
        webViewMap = view.findViewById(R.id.webViewMap)
        mapFragmentContainer = view.findViewById(R.id.mapFragment)

        cardMapInfo = view.findViewById(R.id.cardMapInfo)
        tvMapContactNom = view.findViewById(R.id.tvMapContactNom)
        tvMapCoordinates = view.findViewById(R.id.tvMapCoordinates)
        btnOpenExternalMap = view.findViewById(R.id.btnOpenExternalMap)
        cardApiKeyWarning = view.findViewById(R.id.cardApiKeyWarning)
        cardMapWarning = view.findViewById(R.id.cardMapWarning)

        setupToolbar()
        setupExternalMapButton()
        setupToggleGroup()

        val lat = latitude
        val lon = longitude
        val hasValidCoords = lat != null && lon != null &&
                lat in -90.0..90.0 && lon in -180.0..180.0

        if (hasValidCoords) {
            tvMapContactNom.text = contactNom
            tvMapCoordinates.text = "Lat: %.4f | Lon: %.4f".format(lat!!, lon!!)
            cardMapInfo.visibility = View.VISIBLE
            cardMapWarning.visibility = View.GONE

            // Initialiser la carte avec textures réelles
            setupInteractiveRealMap(lat, lon, contactNom)
        } else {
            cardMapInfo.visibility = View.GONE
            cardMapWarning.visibility = View.VISIBLE
            webViewMap.visibility = View.GONE
            // Message explicite : position absente ou saisie invalide (non numérique / hors limites)
            val invalides = arguments?.getBoolean(ARG_INVALIDES, false) == true ||
                    (lat != null && lon != null)
            view.findViewById<TextView>(R.id.tvMapWarningMessage).setText(
                if (invalides) R.string.msg_coordonnees_invalides else R.string.msg_coordonnees_absentes
            )
        }

        // Initialisation en parallèle du SDK Google Maps
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupToolbar() {
        toolbarCarte.title = getString(R.string.title_carte)
        toolbarCarte.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupToggleGroup() {
        // Sélection par défaut : Carte Réelle avec Textures
        toggleMapType.check(R.id.btnModeInteractive)

        toggleMapType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnModeInteractive -> {
                        webViewMap.visibility = View.VISIBLE
                        mapFragmentContainer.visibility = View.GONE
                        cardApiKeyWarning.visibility = View.GONE
                    }
                    R.id.btnModeGoogleSdk -> {
                        webViewMap.visibility = View.GONE
                        mapFragmentContainer.visibility = View.VISIBLE
                        checkApiKeyConfiguration()
                    }
                }
            }
        }
    }

    /**
     * Charge une carte interactive avec vraies textures (rues, bâtiments, zoom, marqueur)
     * via OpenStreetMap / Leaflet directement dans un WebView sécurisé.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupInteractiveRealMap(lat: Double, lon: Double, nom: String) {
        webViewMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webViewMap.webViewClient = WebViewClient()

        val safeNom = nom.replace("'", "\\'").replace("\"", "\\\"")

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; }
                    .leaflet-popup-content-wrapper { border-radius: 8px; font-family: sans-serif; font-size: 13px; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var lat = $lat;
                    var lon = $lon;
                    var map = L.map('map', { zoomControl: true }).setView([lat, lon], 15);
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '&copy; OpenStreetMap'
                    }).addTo(map);
                    var marker = L.marker([lat, lon]).addTo(map);
                    marker.bindPopup("<b>$safeNom</b><br>Lat: " + lat.toFixed(4) + "<br>Lon: " + lon.toFixed(4)).openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()

        webViewMap.loadDataWithBaseURL("https://www.openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Vérifie si une clé API Google Maps personnalisée est configurée pour le mode SDK.
     */
    private fun checkApiKeyConfiguration() {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
            val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
            if (apiKey.isNullOrEmpty() || apiKey.contains("YOUR_GOOGLE_MAPS_API_KEY_HERE")) {
                cardApiKeyWarning.visibility = View.VISIBLE
            } else {
                cardApiKeyWarning.visibility = View.GONE
            }
        } catch (e: Exception) {
            // Ignorer si échec de lecture
        }
    }

    private fun setupExternalMapButton() {
        btnOpenExternalMap.setOnClickListener {
            val lat = latitude
            val lon = longitude
            if (lat != null && lon != null) {
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($contactNom)")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                try {
                    startActivity(mapIntent)
                } catch (e: Exception) {
                    val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                    startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMapInstance = googleMap
        val lat = latitude
        val lon = longitude

        val hasValidCoords = lat != null && lon != null &&
                lat in -90.0..90.0 && lon in -180.0..180.0

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        if (hasValidCoords) {
            val contactLocation = LatLng(lat!!, lon!!)
            googleMap.addMarker(
                MarkerOptions()
                    .position(contactLocation)
                    .title(contactNom)
                    .snippet("Lat: %.4f | Lon: %.4f".format(lat, lon))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )?.showInfoWindow()

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(contactLocation, 14f))
        } else {
            val defaultLocation = LatLng(46.2276, 2.2137)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
        }
    }

    override fun onDestroyView() {
        webViewMap.stopLoading()
        super.onDestroyView()
    }
}
