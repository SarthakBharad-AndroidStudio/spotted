package com.example.spotted

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ringView: SightingsRingView
    private lateinit var summaryText: TextView
    private lateinit var emptyState: TextView
    private lateinit var sightingList: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: SightingAdapter

    private var sightings: List<Sighting> = emptyList()
    private var sortByRating = false

    private val addLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val sighting = if (android.os.Build.VERSION.SDK_INT >= 33) {

                result.data?.getSerializableExtra(
                    EXTRA_SIGHTING,
                    Sighting::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(
                    EXTRA_SIGHTING
                ) as? Sighting
            }

            if (sighting != null) {

                SightingStore.add(this, sighting)

                refresh()

                Snackbar.make(
                    sightingList,
                    "Spotted: ${sighting.title}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val deletedId =
                result.data?.getStringExtra(EXTRA_DELETED_ID)

            if (deletedId != null) {

                SightingStore.delete(
                    this,
                    deletedId
                )

                refresh()

                Snackbar.make(
                    sightingList,
                    "Sighting deleted",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        toolbar = findViewById(R.id.toolbar)
        ringView = findViewById(R.id.ringView)
        summaryText = findViewById(R.id.summaryText)
        emptyState = findViewById(R.id.emptyState)
        sightingList = findViewById(R.id.sightingList)
        fab = findViewById(R.id.fab)

        setSupportActionBar(toolbar)

        adapter = SightingAdapter(
            emptyList(),
            true
        ) { sighting ->
            openDetail(sighting)
        }

        sightingList.layoutManager =
            LinearLayoutManager(this)

        sightingList.adapter = adapter

        fab.setOnClickListener {
            openAddSighting()
        }
    }

    override fun onResume() {

        super.onResume()

        refresh()
    }

    private fun refresh() {

        val all = SightingStore.load(this)

        sightings =
            if (sortByRating) {

                all.sortedWith(
                    compareByDescending<Sighting> {
                        it.rating
                    }.thenByDescending {
                        it.timestamp
                    }
                )

            } else {

                all.sortedByDescending {
                    it.timestamp
                }
            }

        adapter.submit(
            sightings,
            SightingStore.isShowAddress(this)
        )

        val week =
            SightingStore.thisWeek(all)

        val nightCount =
            week.count { it.isNight }

        val goal =
            SightingStore.getGoal(this)

        ringView.setData(
            week.size,
            nightCount,
            goal
        )

        summaryText.text = buildString {

            append("${week.size} of $goal this week")

            if (nightCount > 0) {
                append(" · $nightCount at night")
            }

            append(" · ${all.size} total")
        }

        emptyState.visibility =
            if (sightings.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun openAddSighting() {

        val week =
            SightingStore.thisWeek(
                SightingStore.load(this)
            )

        val intent =
            Intent(
                this,
                AddSightingActivity::class.java
            ).apply {

                putExtra(
                    EXTRA_WEEK_COUNT,
                    week.size
                )

                putExtra(
                    EXTRA_GOAL,
                    SightingStore.getGoal(
                        this@MainActivity
                    )
                )
            }

        addLauncher.launch(intent)
    }

    private fun openDetail(
        sighting: Sighting
    ) {

        val intent =
            Intent(
                this,
                DetailActivity::class.java
            )

        intent.putExtra(
            EXTRA_SIGHTING,
            sighting
        )

        detailLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.main_menu,
            menu
        )

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        return when (item.itemId) {

            R.id.itemSort -> {

                sortByRating = !sortByRating

                refresh()

                Snackbar.make(
                    sightingList,
                    if (sortByRating) {
                        "Sorted by rating"
                    } else {
                        "Sorted by date"
                    },
                    Snackbar.LENGTH_SHORT
                ).show()

                true
            }

            R.id.itemShareSummary -> {

                shareSummary()

                true
            }

            R.id.itemSettings -> {

                settingsLauncher.launch(
                    Intent(
                        this,
                        SettingsActivity::class.java
                    )
                )

                true
            }

            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun shareSummary() {

        if (sightings.isEmpty()) {

            Snackbar.make(
                sightingList,
                "Nothing to share yet",
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        val text = buildString {

            append(
                "Spotted! — my ${sightings.size} latest finds\n\n"
            )

            sightings
                .take(5)
                .forEach {

                    append(
                        "• ${it.title} " +
                                "(${it.category}) — " +
                                "${it.shortLocation()}\n"
                    )
                }
        }

        val intent =
            Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Spotted!"
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

    companion object {

        const val EXTRA_SIGHTING =
            "extra_sighting"

        const val EXTRA_DELETED_ID =
            "extra_deleted_id"

        const val EXTRA_WEEK_COUNT =
            "extra_week_count"

        const val EXTRA_GOAL =
            "extra_goal"
    }
}