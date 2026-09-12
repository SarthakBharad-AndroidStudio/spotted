package com.example.spotted

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var sighting: Sighting

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_detail
        )

        val incoming: Sighting? =
            if (android.os.Build.VERSION.SDK_INT >= 33) {

                intent.getSerializableExtra(
                    MainActivity.EXTRA_SIGHTING,
                    Sighting::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getSerializableExtra(
                    MainActivity.EXTRA_SIGHTING
                ) as? Sighting
            }

        if (incoming == null) {

            finish()

            return
        }

        sighting = incoming

        val toolbar: MaterialToolbar =
            findViewById(R.id.detailToolbar)

        setSupportActionBar(toolbar)

        toolbar.title =
            sighting.title

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val photo: ImageView =
            findViewById(R.id.detailPhoto)

        val title: TextView =
            findViewById(R.id.detailTitle)

        val category: TextView =
            findViewById(R.id.detailCategory)

        val rating: RatingBar =
            findViewById(R.id.detailRating)

        val note: TextView =
            findViewById(R.id.detailNote)

        val whenText: TextView =
            findViewById(R.id.detailWhen)

        val place: TextView =
            findViewById(R.id.detailPlace)

        val light: TextView =
            findViewById(R.id.detailLight)

        val mapButton: Button =
            findViewById(R.id.mapButton)

        val shareButton: Button =
            findViewById(R.id.shareButton)

        title.text =
            sighting.title

        category.text =
            sighting.category

        rating.rating =
            sighting.rating

        note.text =
            sighting.note

        note.visibility =
            if (sighting.note.isBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        whenText.text =
            sighting.formattedDate()

        place.text =
            sighting.shortLocation()

        light.text =
            String.format(
                Locale.US,
                "%.0f lx · %s",
                sighting.lux,
                sighting.lightLabel
            )

        val bitmap =
            PhotoStorage.decodeScaled(
                sighting.photoPath,
                900
            )

        if (bitmap != null) {

            photo.setImageBitmap(bitmap)

        } else {

            photo.setImageResource(
                R.drawable.ic_photo
            )
        }

        mapButton.isEnabled =
            sighting.hasLocation

        mapButton.setOnClickListener {
            showOnMap()
        }

        shareButton.setOnClickListener {
            share()
        }
    }

    private fun showOnMap() {

        val label =
            Uri.encode(sighting.title)

        val uri =
            Uri.parse(
                String.format(
                    Locale.US,
                    "geo:%f,%f?q=%f,%f(%s)",
                    sighting.latitude,
                    sighting.longitude,
                    sighting.latitude,
                    sighting.longitude,
                    label
                )
            )

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )

        } catch (
            e: ActivityNotFoundException
        ) {

            Snackbar.make(
                findViewById(R.id.detailRoot),
                "No map app installed on this device",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun share() {

        val text =
            buildString {

                append(
                    "Spotted: ${sighting.title}\n"
                )

                append(
                    "${sighting.category} · " +
                            "${sighting.formattedDate()}\n"
                )

                if (sighting.hasLocation) {

                    append(
                        "${sighting.shortLocation()}\n"
                    )
                }

                append(
                    "Light: ${sighting.lightLabel}\n"
                )

                if (sighting.note.isNotBlank()) {

                    append(
                        "\n${sighting.note}"
                    )
                }
            }

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Spotted: ${sighting.title}"
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    text
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                getString(R.string.share)
            )
        )
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.detailed_menu,
            menu
        )

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        if (item.itemId ==
            R.id.itemDelete
        ) {

            confirmDelete()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun confirmDelete() {

        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(
                "Delete \"${sighting.title}\"?"
            )
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->

                val data =
                    Intent().putExtra(
                        MainActivity.EXTRA_DELETED_ID,
                        sighting.id
                    )

                setResult(
                    Activity.RESULT_OK,
                    data
                )

                finish()
            }
            .setNegativeButton(
                android.R.string.cancel,
                null
            )
            .show()
    }
}