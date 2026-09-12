package com.example.spotted

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One logged sighting.
 *
 * Serializable allows the object to be passed between Activities.
 */
data class Sighting(
    val id: String,
    val title: String,
    val note: String,
    val category: String,
    val rating: Float,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val lux: Float,
    val photoPath: String?
) : Serializable {

    val lightLabel: String
        get() = when {
            lux < NIGHT_LUX -> "Night"
            lux < DUSK_LUX -> "Dusk"
            lux < INDOOR_LUX -> "Indoor"
            else -> "Daylight"
        }

    val isNight: Boolean
        get() = lux < NIGHT_LUX

    val hasLocation: Boolean
        get() = latitude != 0.0 || longitude != 0.0

    fun formattedDate(): String =
        SimpleDateFormat(
            "dd.MM.yyyy · HH:mm",
            Locale.getDefault()
        ).format(Date(timestamp))

    fun shortLocation(): String = when {
        address.isNotBlank() -> address
        hasLocation -> String.format(
            Locale.US,
            "%.4f, %.4f",
            latitude,
            longitude
        )
        else -> "No location"
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        const val NIGHT_LUX = 12f
        const val DUSK_LUX = 60f
        const val INDOOR_LUX = 800f

        val CATEGORIES = listOf(
            "Street art",
            "Animal",
            "Vehicle",
            "Sign",
            "Plant",
            "Other"
        )
    }
}