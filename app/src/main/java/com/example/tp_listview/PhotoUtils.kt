package com.example.tp_listview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File

/**
 * Gestion des photos de contacts.
 *
 * Une URI de galerie (content://) est temporaire : elle peut devenir illisible après
 * redémarrage. On copie donc toujours l'image dans filesDir/photos et on enregistre en
 * base le chemin absolu de cette copie.
 */
object PhotoUtils {

    private const val DOSSIER = "photos"
    private const val TAILLE_MAX_PX = 512

    private fun nouveauFichier(context: Context): File {
        val dossier = File(context.filesDir, DOSSIER).apply { mkdirs() }
        return File(dossier, "contact_${System.currentTimeMillis()}.jpg")
    }

    /** Copie une image (galerie) dans le stockage interne, redimensionnée. À appeler hors thread principal. */
    fun copierDepuisUri(context: Context, source: Uri): String {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Image illisible")
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculerEchantillonnage(options.outWidth, options.outHeight)
        }
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("Image illisible")
        return enregistrerBitmap(context, bitmap)
    }

    /** Enregistre un Bitmap (caméra) dans le stockage interne. À appeler hors thread principal. */
    fun enregistrerBitmap(context: Context, bitmap: Bitmap): String {
        val fichier = nouveauFichier(context)
        fichier.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return fichier.absolutePath
    }

    /** Affiche la photo d'un contact ou l'avatar par défaut si elle est absente / illisible. */
    fun afficher(imageView: ImageView, photo: String?) {
        if (photo.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_default_avatar)
            return
        }
        try {
            if (photo.startsWith("android.resource://")) {
                imageView.setImageURI(Uri.parse(photo))
            } else {
                val fichier = File(photo)
                val bitmap = if (fichier.exists()) BitmapFactory.decodeFile(fichier.absolutePath) else null
                if (bitmap != null) imageView.setImageBitmap(bitmap)
                else imageView.setImageResource(R.drawable.ic_default_avatar)
            }
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun calculerEchantillonnage(largeur: Int, hauteur: Int): Int {
        var echantillon = 1
        while (largeur / (echantillon * 2) >= TAILLE_MAX_PX && hauteur / (echantillon * 2) >= TAILLE_MAX_PX) {
            echantillon *= 2
        }
        return echantillon
    }

    /** Supprime une photo temporaire (copiée puis abandonnée via Annuler). */
    fun supprimerSiInterne(context: Context, chemin: String?) {
        if (chemin.isNullOrBlank()) return
        val fichier = File(chemin)
        if (fichier.absolutePath.startsWith(context.filesDir.absolutePath)) fichier.delete()
    }
}
