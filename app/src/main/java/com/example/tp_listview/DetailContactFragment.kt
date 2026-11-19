package com.example.tp_listview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.tp_listview.ContactViewModel.ResultatOperation
import com.example.tp_listview.data.Contact
import com.example.tp_listview.data.ContactValidator
import com.example.tp_listview.data.ContactValidator.ResultatGps
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Création, consultation, modification et suppression d'un contact.
 *
 * - Mode création : aucun identifiant dans le Bundle (champs vides).
 * - Mode édition : l'_id de base est transmis par Bundle, le contact est relu depuis SQLite.
 * - Rotation : les EditText restaurent leur saisie eux-mêmes ; on ne recharge depuis la base
 *   qu'à la première création (savedInstanceState == null) pour ne pas écraser la saisie.
 */
class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    companion object {
        private const val ARG_CONTACT_ID = "arg_contact_id"
        private const val ETAT_PHOTO_COURANTE = "etat_photo_courante"
        private const val ETAT_PHOTO_ORIGINALE = "etat_photo_originale"
        private const val ETAT_PHOTOS_TEMPORAIRES = "etat_photos_temporaires"

        fun newInstance(contactId: Long?): DetailContactFragment =
            DetailContactFragment().apply {
                arguments = Bundle().apply { if (contactId != null) putLong(ARG_CONTACT_ID, contactId) }
            }
    }

    private val viewModel: ContactViewModel by activityViewModels()

    /** 0 = mode création ; sinon _id du contact en base. */
    private var contactId: Long = 0L
    private val modeCreation get() = contactId == 0L

    /** Photo actuellement affichée (chemin filesDir ou URI ressource). */
    private var photoCourante: String? = null
    /** Photo enregistrée en base avant modification. */
    private var photoOriginale: String? = null
    /** Copies créées pendant la saisie, supprimées si l'utilisateur annule. */
    private val photosTemporaires = ArrayList<String>()

    private lateinit var toolbarDetail: MaterialToolbar
    private lateinit var ivDetailPhoto: ImageView
    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etTelephone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var btnEnregistrer: MaterialButton

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) {
                toast(R.string.msg_photo_annulee)
            } else {
                traiterNouvellePhoto { PhotoUtils.copierDepuisUri(requireContext().applicationContext, uri) }
            }
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap == null) {
                toast(R.string.msg_camera_annulee)
            } else {
                traiterNouvellePhoto { PhotoUtils.enregistrerBitmap(requireContext().applicationContext, bitmap) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = arguments?.getLong(ARG_CONTACT_ID, 0L) ?: 0L
        savedInstanceState?.let {
            photoCourante = it.getString(ETAT_PHOTO_COURANTE)
            photoOriginale = it.getString(ETAT_PHOTO_ORIGINALE)
            it.getStringArrayList(ETAT_PHOTOS_TEMPORAIRES)?.let(photosTemporaires::addAll)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_PHOTO_COURANTE, photoCourante)
        outState.putString(ETAT_PHOTO_ORIGINALE, photoOriginale)
        outState.putStringArrayList(ETAT_PHOTOS_TEMPORAIRES, photosTemporaires)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarDetail = view.findViewById(R.id.toolbarDetail)
        ivDetailPhoto = view.findViewById(R.id.ivDetailPhoto)
        etNom = view.findViewById(R.id.etNom)
        etPrenom = view.findViewById(R.id.etPrenom)
        etTelephone = view.findViewById(R.id.etTelephone)
        etEmail = view.findViewById(R.id.etEmail)
        etLatitude = view.findViewById(R.id.etLatitude)
        etLongitude = view.findViewById(R.id.etLongitude)
        btnEnregistrer = view.findViewById(R.id.btnEnregistrer)

        setupToolbar()
        appliquerPreferences()
        setupListeners(view)

        if (savedInstanceState == null && !modeCreation) {
            chargerContact()
        } else {
            PhotoUtils.afficher(ivDetailPhoto, photoCourante)
        }
    }

    private fun setupToolbar() {
        toolbarDetail.setNavigationOnClickListener { annuler() }
        toolbarDetail.title = getString(if (modeCreation) R.string.title_nouveau_contact else R.string.title_detail)
        toolbarDetail.menu.findItem(R.id.action_delete)?.isVisible = !modeCreation
        toolbarDetail.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_delete) {
                confirmerSuppression(); true
            } else false
        }
    }

    /** Taille de police (nom, prénom, téléphone) et couleur (nom, prénom) issues des préférences. */
    private fun appliquerPreferences() {
        val affichage = PreferencesManager(requireContext()).lireAffichage()
        listOf(etNom, etPrenom, etTelephone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, affichage.tailleSp)
        }
        etNom.setTextColor(affichage.couleurTexte)
        etPrenom.setTextColor(affichage.couleurTexte)
    }

    private fun chargerContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            if (contact == null) {
                toast(R.string.msg_contact_introuvable)
                parentFragmentManager.popBackStack()
                return@launch
            }
            etNom.setText(contact.nom)
            etPrenom.setText(contact.prenom)
            etTelephone.setText(contact.telephone)
            etEmail.setText(contact.email.orEmpty())
            etLatitude.setText(contact.latitude?.toString().orEmpty())
            etLongitude.setText(contact.longitude?.toString().orEmpty())
            photoOriginale = contact.photoUri
            photoCourante = contact.photoUri
            PhotoUtils.afficher(ivDetailPhoto, photoCourante)
        }
    }

    private fun setupListeners(view: View) {
        view.findViewById<ImageButton>(R.id.btnAppeler).setOnClickListener {
            val tel = etTelephone.text.toString().trim()
            if (tel.isEmpty()) {
                etTelephone.error = getString(R.string.err_telephone_vide)
                return@setOnClickListener
            }
            lancerIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(tel)}")), R.string.msg_aucune_app_tel)
        }

        view.findViewById<ImageButton>(R.id.btnEmail).setOnClickListener {
            val mail = etEmail.text.toString().trim()
            if (mail.isEmpty()) {
                etEmail.error = getString(R.string.err_email_vide)
                return@setOnClickListener
            }
            lancerIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail")), R.string.msg_aucune_app_mail)
        }

        view.findViewById<MaterialButton>(R.id.btnChoisirPhoto).setOnClickListener {
            try {
                pickImageLauncher.launch("image/*")
            } catch (e: ActivityNotFoundException) {
                toast(R.string.msg_galerie_indisponible)
            }
        }

        val lancerCamera = View.OnClickListener {
            try {
                takePhotoLauncher.launch(null)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.msg_camera_indisponible)
            }
        }
        view.findViewById<MaterialButton>(R.id.btnPrendrePhoto).setOnClickListener(lancerCamera)
        view.findViewById<ImageView>(R.id.ivCameraBadge).setOnClickListener(lancerCamera)

        view.findViewById<MaterialButton>(R.id.btnVoirSurCarte).setOnClickListener { ouvrirCarte() }
        btnEnregistrer.setOnClickListener { enregistrerContact() }
        view.findViewById<MaterialButton>(R.id.btnAnnuler).setOnClickListener { annuler() }
    }

    /** Copie de l'image hors du thread principal, puis affichage. */
    private fun traiterNouvellePhoto(copie: () -> String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val chemin = try {
                withContext(Dispatchers.IO) { copie() }
            } catch (e: Exception) {
                null
            }
            if (chemin == null) {
                toast(R.string.msg_photo_illisible)
            } else {
                photosTemporaires.add(chemin)
                photoCourante = chemin
                PhotoUtils.afficher(ivDetailPhoto, chemin)
            }
        }
    }

    /** Ouvre toujours la carte : elle affiche un message explicite si la position est absente ou invalide. */
    private fun ouvrirCarte() {
        val nomAffiche = "${etNom.text.toString().trim()} ${etPrenom.text.toString().trim()}".trim()
        val latSaisie = etLatitude.text.toString()
        val lonSaisie = etLongitude.text.toString()
        val (lat, lon, saisiePresente) = when (val r = ContactValidator.analyserGps(latSaisie, lonSaisie)) {
            is ResultatGps.Valide -> Triple(r.latitude, r.longitude, true)
            ResultatGps.Absent -> Triple(null, null, false)
            is ResultatGps.Invalide -> Triple(null, null, true)
        }
        val carte = CarteContactFragment.newInstance(
            contactId = contactId.takeIf { it != 0L },
            contactNom = nomAffiche.ifEmpty { getString(R.string.contact_sans_nom) },
            latitude = lat,
            longitude = lon,
            coordonneesInvalides = saisiePresente && lat == null
        )
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, carte)
            .addToBackStack(null)
            .commit()
    }

    private fun enregistrerContact() {
        val nom = etNom.text.toString().trim()
        val prenom = etPrenom.text.toString().trim()
        val telephone = etTelephone.text.toString().trim()
        val email = etEmail.text.toString().trim()

        var erreur = false
        if (nom.isEmpty()) { etNom.error = getString(R.string.err_nom_vide); erreur = true }
        if (prenom.isEmpty()) { etPrenom.error = getString(R.string.err_prenom_vide); erreur = true }
        if (telephone.isEmpty()) { etTelephone.error = getString(R.string.err_telephone_vide); erreur = true }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.err_email_invalide); erreur = true
        }

        var latitude: Double? = null
        var longitude: Double? = null
        when (val gps = ContactValidator.analyserGps(etLatitude.text.toString(), etLongitude.text.toString())) {
            ResultatGps.Absent -> Unit
            is ResultatGps.Valide -> { latitude = gps.latitude; longitude = gps.longitude }
            is ResultatGps.Invalide -> {
                if (gps.latitudeEnErreur) etLatitude.error = getString(R.string.err_latitude_invalide)
                if (gps.longitudeEnErreur) etLongitude.error = getString(R.string.err_longitude_invalide)
                erreur = true
            }
        }

        if (erreur) {
            toast(R.string.msg_corriger_erreurs)
            return  // rien n'est écrit en base
        }

        val contact = Contact(
            id = contactId,
            nom = nom,
            prenom = prenom,
            telephone = telephone,
            email = email.ifEmpty { null },
            photoUri = photoCourante,
            latitude = latitude,
            longitude = longitude
        )

        btnEnregistrer.isEnabled = false  // évite un double enregistrement
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultat = viewModel.enregistrer(contact, photoOriginale)) {
                is ResultatOperation.Succes -> {
                    // Les copies non retenues sont supprimées ; la photo choisie est conservée.
                    photosTemporaires.filter { it != photoCourante }
                        .forEach { PhotoUtils.supprimerSiInterne(requireContext(), it) }
                    photosTemporaires.clear()
                    val msg = if (modeCreation) R.string.msg_contact_ajoute else R.string.msg_contact_modifie
                    Toast.makeText(requireContext(), getString(msg, contact.nomComplet), Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                ResultatOperation.EchecInsertion -> { toast(R.string.err_insertion); btnEnregistrer.isEnabled = true }
                ResultatOperation.Introuvable -> { toast(R.string.msg_contact_introuvable); btnEnregistrer.isEnabled = true }
                is ResultatOperation.Erreur -> {
                    Toast.makeText(requireContext(), getString(R.string.err_base, resultat.message), Toast.LENGTH_LONG).show()
                    btnEnregistrer.isEnabled = true
                }
            }
        }
    }

    private fun confirmerSuppression() {
        val nomAffiche = "${etNom.text.toString().trim()} ${etPrenom.text.toString().trim()}".trim()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dlg_supprimer_titre)
            .setMessage(getString(R.string.dlg_supprimer_msg, nomAffiche))
            .setPositiveButton(R.string.dlg_oui) { _, _ -> supprimer() }
            .setNegativeButton(R.string.dlg_non, null)
            .show()
    }

    private fun supprimer() {
        viewLifecycleOwner.lifecycleScope.launch {
            val aSupprimer = Contact(id = contactId, nom = "", prenom = "", telephone = "", photoUri = photoOriginale)
            when (val resultat = viewModel.supprimer(aSupprimer)) {
                is ResultatOperation.Succes -> {
                    photosTemporaires.forEach { PhotoUtils.supprimerSiInterne(requireContext(), it) }
                    photosTemporaires.clear()
                    toast(R.string.msg_contact_supprime)
                    parentFragmentManager.popBackStack()
                }
                ResultatOperation.Introuvable -> {
                    toast(R.string.msg_contact_introuvable)
                    parentFragmentManager.popBackStack()
                }
                is ResultatOperation.Erreur ->
                    Toast.makeText(requireContext(), getString(R.string.err_base, resultat.message), Toast.LENGTH_LONG).show()
                ResultatOperation.EchecInsertion -> Unit
            }
        }
    }

    /** Annuler : rien n'est enregistré ; les photos copiées pendant la saisie sont supprimées. */
    private fun annuler() {
        photosTemporaires.forEach { PhotoUtils.supprimerSiInterne(requireContext(), it) }
        photosTemporaires.clear()
        parentFragmentManager.popBackStack()
    }

    /** Retour système (geste / bouton) : même nettoyage que Annuler. Pas exécuté lors d'une rotation. */
    override fun onDestroy() {
        if (isRemoving && photosTemporaires.isNotEmpty()) {
            context?.let { ctx -> photosTemporaires.forEach { PhotoUtils.supprimerSiInterne(ctx, it) } }
            photosTemporaires.clear()
        }
        super.onDestroy()
    }

    private fun lancerIntent(intent: Intent, messageIndisponible: Int) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(messageIndisponible)
        }
    }

    private fun toast(res: Int) {
        context?.let { Toast.makeText(it, res, Toast.LENGTH_SHORT).show() }
    }
}
