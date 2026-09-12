package com.example.spotted

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * All persistence for the app. Sightings live as a JSON array inside a single
 * SharedPreferences string; photos live as files and are referenced here by path.
 */
object SightingStore {

    private const val PREFS = "spotted_prefs"
    private const val KEY_SIGHTINGS = "sightings"
    private const val KEY_GOAL = "weekly_goal"
    private const val KEY_ATTACH_LOCATION = "attach_location"
    private const val KEY_SHOW_ADDRESS = "show_address"

    const val DEFAULT_GOAL = 7

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    // ---------- sightings ----------

    fun load(context: Context): MutableList<Sighting> {

        val raw = prefs(context)
            .getString(KEY_SIGHTINGS, null)
            ?: return mutableListOf()

        val out = mutableListOf<Sighting>()

        try {
            val array = JSONArray(raw)

            for (i in 0 until array.length()) {
                out.add(
                    fromJson(
                        array.getJSONObject(i)
                    )
                )
            }

        } catch (e: Exception) {
            // Corrupt store — start clean rather than crashing on launch.
            return mutableListOf()
        }

        return out
    }

    fun save(
        context: Context,
        sightings: List<Sighting>
    ) {
        val array = JSONArray()

        sightings.forEach {
            array.put(toJson(it))
        }

        prefs(context)
            .edit()
            .putString(KEY_SIGHTINGS, array.toString())
            .apply()
    }

    fun add(
        context: Context,
        sighting: Sighting
    ) {
        val all = load(context)

        all.add(0, sighting)

        save(context, all)
    }

    fun delete(
        context: Context,
        id: String
    ) {
        val all = load(context)

        val removed = all.filter {
            it.id == id
        }

        all.removeAll {
            it.id == id
        }

        save(context, all)

        // Don't leave orphaned image files behind.
        removed.forEach {
            PhotoStorage.delete(it.photoPath)
        }
    }

    /**
     * Sightings from the last 7 days.
     */
    fun thisWeek(
        sightings: List<Sighting>
    ): List<Sighting> {

        val cutoff = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis

        return sightings.filter {
            it.timestamp >= cutoff
        }
    }

    // ---------- settings ----------

    fun getGoal(context: Context): Int =
        prefs(context).getInt(
            KEY_GOAL,
            DEFAULT_GOAL
        )

    fun setGoal(
        context: Context,
        goal: Int
    ) =
        prefs(context)
            .edit()
            .putInt(KEY_GOAL, goal)
            .apply()

    fun isAttachLocation(
        context: Context
    ): Boolean =
        prefs(context).getBoolean(
            KEY_ATTACH_LOCATION,
            true
        )

    fun setAttachLocation(
        context: Context,
        value: Boolean
    ) =
        prefs(context)
            .edit()
            .putBoolean(KEY_ATTACH_LOCATION, value)
            .apply()

    fun isShowAddress(
        context: Context
    ): Boolean =
        prefs(context).getBoolean(
            KEY_SHOW_ADDRESS,
            true
        )

    fun setShowAddress(
        context: Context,
        value: Boolean
    ) =
        prefs(context)
            .edit()
            .putBoolean(KEY_SHOW_ADDRESS, value)
            .apply()

    // ---------- JSON mapping ----------

    private fun toJson(
        s: Sighting
    ): JSONObject =
        JSONObject().apply {

            put("id", s.id)
            put("title", s.title)
            put("note", s.note)
            put("category", s.category)
            put("rating", s.rating.toDouble())
            put("timestamp", s.timestamp)
            put("lat", s.latitude)
            put("lon", s.longitude)
            put("address", s.address)
            put("lux", s.lux.toDouble())
            put("photo", s.photoPath ?: "")
        }

    private fun fromJson(
        o: JSONObject
    ): Sighting =
        Sighting(
            id = o.getString("id"),
            title = o.optString("title"),
            note = o.optString("note"),
            category = o.optString(
                "category",
                "Other"
            ),
            rating = o.optDouble(
                "rating",
                0.0
            ).toFloat(),
            timestamp = o.optLong(
                "timestamp",
                System.currentTimeMillis()
            ),
            latitude = o.optDouble(
                "lat",
                0.0
            ),
            longitude = o.optDouble(
                "lon",
                0.0
            ),
            address = o.optString("address"),
            lux = o.optDouble(
                "lux",
                0.0
            ).toFloat(),
            photoPath = o.optString("photo")
                .takeIf { it.isNotBlank() }
        )
}