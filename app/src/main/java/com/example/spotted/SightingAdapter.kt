package com.example.spotted

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for the gallery.
 */
class SightingAdapter(
    private var items: List<Sighting>,
    private var showAddress: Boolean,
    private val onItemClick: OnSightingClickListener
) : RecyclerView.Adapter<SightingAdapter.ViewHolder>() {

    fun interface OnSightingClickListener {
        fun onSightingClick(sighting: Sighting)
    }

    class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val photo: ImageView =
            view.findViewById(R.id.itemPhoto)

        val title: TextView =
            view.findViewById(R.id.itemTitle)

        val meta: TextView =
            view.findViewById(R.id.itemMeta)

        val address: TextView =
            view.findViewById(R.id.itemAddress)

        val rating: RatingBar =
            view.findViewById(R.id.itemRating)

        val lightIcon: ImageView =
            view.findViewById(R.id.itemLightIcon)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_sighting,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val sighting =
            items[position]

        holder.title.text =
            sighting.title

        holder.meta.text =
            "${sighting.category} · ${sighting.formattedDate()}"

        holder.rating.rating =
            sighting.rating

        if (
            showAddress &&
            sighting.hasLocation
        ) {

            holder.address.visibility =
                View.VISIBLE

            holder.address.text =
                sighting.shortLocation()

        } else {

            holder.address.visibility =
                View.GONE
        }

        holder.lightIcon.setImageResource(
            if (sighting.isNight) {
                R.drawable.ic_night
            } else {
                R.drawable.ic_light
            }
        )

        val bitmap =
            PhotoStorage.decodeScaled(
                sighting.photoPath,
                200
            )

        if (bitmap != null) {

            holder.photo.setImageBitmap(
                bitmap
            )

        } else {

            holder.photo.setImageResource(
                R.drawable.ic_photo
            )
        }

        holder.itemView.setOnClickListener {
            onItemClick.onSightingClick(
                sighting
            )
        }
    }

    override fun getItemCount(): Int =
        items.size

    fun submit(
        newItems: List<Sighting>,
        showAddress: Boolean
    ) {

        this.items =
            newItems

        this.showAddress =
            showAddress

        notifyDataSetChanged()
    }
}