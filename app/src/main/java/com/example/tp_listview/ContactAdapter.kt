package com.example.tp_listview

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tp_listview.data.Contact

/**
 * Adapter de la RecyclerView.
 *
 * - Les contacts sont identifiés par leur _id de base (stableIds + DiffUtil), jamais par leur position.
 * - Reçoit des préférences déjà converties (taille en sp, couleur Int, booléen) via [appliquerAffichage].
 */
class ContactAdapter(
    private val onContactClick: (contactId: Long) -> Unit,
    private val onAppelClick: (telephone: String) -> Unit,
    private val onFavoriClick: ((contact: Contact) -> Unit)? = null
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    private var affichage = PreferencesManager.Affichage(
        tailleSp = PreferencesManager.TAILLE_DEFAUT_SP,
        couleurTexte = android.graphics.Color.BLACK,
        appelDepuisListe = PreferencesManager.APPEL_DEFAUT
    )

    init {
        setHasStableIds(true)
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivItemPhoto)
        val tvNomPrenom: TextView = itemView.findViewById(R.id.tvItemNomPrenom)
        val tvTelephone: TextView = itemView.findViewById(R.id.tvItemTelephone)
        val tvGpsBadge: TextView = itemView.findViewById(R.id.tvItemGpsBadge)
        val btnFavori: ImageButton = itemView.findViewById(R.id.btnItemFavori)
        val btnAppeler: ImageButton = itemView.findViewById(R.id.btnItemAppeler)
        val ivChevron: ImageView = itemView.findViewById(R.id.ivItemChevron)
    }

    /** Applique de nouvelles préférences ; ne redessine la liste que si elles ont changé. */
    fun appliquerAffichage(nouvelAffichage: PreferencesManager.Affichage) {
        if (nouvelAffichage == affichage) return
        affichage = nouvelAffichage
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        val holder = ContactViewHolder(view)

        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onContactClick(getItem(position).id)
        }
        // Le bouton consomme son propre clic : il n'ouvre pas la fiche détaillée.
        holder.btnAppeler.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onAppelClick(getItem(position).telephone)
        }
        holder.btnFavori.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onFavoriClick?.invoke(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)

        holder.tvNomPrenom.text = contact.nomComplet
        holder.tvTelephone.text = contact.telephone

        holder.tvNomPrenom.setTextSize(TypedValue.COMPLEX_UNIT_SP, affichage.tailleSp)
        holder.tvTelephone.setTextSize(TypedValue.COMPLEX_UNIT_SP, affichage.tailleSp)
        holder.tvNomPrenom.setTextColor(affichage.couleurTexte)

        holder.tvGpsBadge.visibility = if (contact.hasValidCoordinates) View.VISIBLE else View.GONE

        holder.btnAppeler.visibility = if (affichage.appelDepuisListe) View.VISIBLE else View.GONE
        holder.ivChevron.visibility = if (affichage.appelDepuisListe) View.GONE else View.VISIBLE
        holder.btnAppeler.contentDescription =
            holder.itemView.context.getString(R.string.cd_appeler_contact, contact.nomComplet)

        PhotoUtils.afficher(holder.ivPhoto, contact.photoUri)

        // Affichage état favori (Bonus)
        holder.btnFavori.setImageResource(
            if (contact.favori) R.drawable.ic_star_filled else R.drawable.ic_star_border
        )
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(oldItem: Contact, newItem: Contact) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Contact, newItem: Contact) = oldItem == newItem
        }
    }
}
