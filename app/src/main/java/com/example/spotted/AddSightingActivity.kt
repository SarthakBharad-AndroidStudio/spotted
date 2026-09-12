package com.example.spotted

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.log10
import kotlin.math.min
import kotlin.concurrent.thread

class AddSightingActivity :
    AppCompatActivity(),
    LocationListener,
    SensorEventListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressHint: TextView
    private lateinit var titleInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var ratingInput: RatingBar
    private lateinit var photoPreview: ImageView
    private lateinit var takePhotoButton: Button
    private lateinit var pickPhotoButton: Button
    private lateinit var attachLocationSwitch: MaterialSwitch
    private lateinit var coordsText: TextView
    private lateinit var addressText: TextView
    private lateinit var lightText: TextView
    private lateinit var lightBar: ProgressBar
    private lateinit var saveButton: Button

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager

    private var lightSensor: Sensor? = null

    private var currentLocation: Location? = null
    private var currentAddress: String = ""
    private var currentLux: Float = 0f
    private var photoPath: String? = null

    private var pendingCapture: File? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->

            val ok =
                granted[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true ||
                        granted[
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ] == true

            if (ok) {

                startLocationUpdates()

            } else if (
                !shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

                AlertDialog.Builder(this)
                    .setTitle(R.string.label_location)
                    .setMessage(R.string.perm_denied_forever)
                    .setPositiveButton(
                        R.string.settings
                    ) { _, _ ->

                        val intent =
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {

                                data = Uri.fromParts(
                                    "package",
                                    packageName,
                                    null
                                )
                            }

                        startActivity(intent)
                    }
                    .setNegativeButton(
                        android.R.string.cancel,
                        null
                    )
                    .show()

            } else {

                coordsText.setText(
                    R.string.no_permission
                )
            }
        }

    private val photoLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val uri: Uri? =
                    result.data?.data

                if (uri != null) {

                    photoPath =
                        PhotoStorage.store(
                            this,
                            uri
                        )

                    val bitmap =
                        PhotoStorage.decodeScaled(
                            photoPath,
                            300
                        )

                    if (bitmap != null) {
                        photoPreview.setImageBitmap(
                            bitmap
                        )
                    }
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val captured = pendingCapture

            pendingCapture = null

            if (
                result.resultCode == Activity.RESULT_OK &&
                captured != null
            ) {

                photoPath =
                    PhotoStorage.store(
                        this,
                        captureUri(captured)
                    )

                // The copy in filesDir is the one that outlives the cache.
                captured.delete()

                val bitmap =
                    PhotoStorage.decodeScaled(
                        photoPath,
                        300
                    )

                if (bitmap != null) {
                    photoPreview.setImageBitmap(
                        bitmap
                    )
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_add_sighting
        )

        toolbar =
            findViewById(R.id.addToolbar)

        progressHint =
            findViewById(R.id.progressHint)

        titleInput =
            findViewById(R.id.titleInput)

        noteInput =
            findViewById(R.id.noteInput)

        categorySpinner =
            findViewById(R.id.categorySpinner)

        ratingInput =
            findViewById(R.id.ratingInput)

        photoPreview =
            findViewById(R.id.photoPreview)

        takePhotoButton =
            findViewById(R.id.takePhotoButton)

        pickPhotoButton =
            findViewById(R.id.pickPhotoButton)

        attachLocationSwitch =
            findViewById(R.id.attachLocationSwitch)

        coordsText =
            findViewById(R.id.coordsText)

        addressText =
            findViewById(R.id.addressText)

        lightText =
            findViewById(R.id.lightText)

        lightBar =
            findViewById(R.id.lightBar)

        saveButton =
            findViewById(R.id.saveButton)

        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val weekCount =
            intent.getIntExtra(
                MainActivity.EXTRA_WEEK_COUNT,
                0
            )

        val goal =
            intent.getIntExtra(
                MainActivity.EXTRA_GOAL,
                SightingStore.DEFAULT_GOAL
            )

        progressHint.text =
            getString(
                R.string.progress_hint,
                weekCount + 1,
                goal
            )

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        sensorManager =
            getSystemService(
                SENSOR_SERVICE
            ) as SensorManager

        lightSensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_LIGHT
            )

        if (lightSensor == null) {

            lightText.text =
                "No light sensor on this device"
        }

        attachLocationSwitch.isChecked =
            SightingStore.isAttachLocation(this)

        attachLocationSwitch
            .setOnCheckedChangeListener { _, checked ->

                if (checked) {

                    getLocation()

                } else {

                    locationManager.removeUpdates(
                        this
                    )

                    currentLocation = null
                    currentAddress = ""

                    coordsText.setText(
                        R.string.location_off
                    )

                    addressText.text = ""
                }
            }

        takePhotoButton.setOnClickListener {
            takePhoto()
        }

        pickPhotoButton.setOnClickListener {
            openGallery()
        }

        saveButton.setOnClickListener {
            saveSighting()
        }
    }

    override fun onResume() {

        super.onResume()

        lightSensor?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (attachLocationSwitch.isChecked) {
            getLocation()
        }
    }

    override fun onPause() {

        super.onPause()

        sensorManager.unregisterListener(this)

        locationManager.removeUpdates(this)
    }

    private fun getLocation() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
        ) {

            startLocationUpdates()

            return
        }

        if (
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {

            AlertDialog.Builder(this)
                .setTitle(
                    R.string.label_location
                )
                .setMessage(
                    R.string.perm_rationale
                )
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    requestLocationPermission()
                }
                .setNegativeButton(
                    android.R.string.cancel,
                    null
                )
                .show()

        } else {

            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startLocationUpdates() {

        val provider = when {

            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ->
                LocationManager.GPS_PROVIDER

            locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            ) ->
                LocationManager.NETWORK_PROVIDER

            else ->
                null
        }

        if (provider == null) {

            coordsText.setText(
                R.string.no_location
            )

            return
        }

        try {

            locationManager
                .getLastKnownLocation(provider)
                ?.let {
                    onLocationChanged(it)
                }

            locationManager.requestLocationUpdates(
                provider,
                2000L,
                5f,
                this
            )

        } catch (e: SecurityException) {

            coordsText.setText(
                R.string.no_permission
            )
        }
    }

    override fun onLocationChanged(
        location: Location
    ) {

        currentLocation = location

        coordsText.text =
            String.format(
                Locale.US,
                "%.5f, %.5f  (±%.0f m)",
                location.latitude,
                location.longitude,
                location.accuracy
            )

        resolveAddress(location)
    }

    /**
     * Reverse-geocodes the GPS location.
     *
     * Android 13+ (API 33+) has the asynchronous Geocoder
     * listener API.
     *
     * Android 11/12 use the older API, but we run it on a
     * background thread so the UI does not freeze.
     */
    private fun resolveAddress(
        location: Location
    ) {

        if (!Geocoder.isPresent()) {
            return
        }

        val geocoder =
            Geocoder(
                this,
                Locale.getDefault()
            )

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            try {

                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->

                    val address =
                        addresses.firstOrNull()
                            ?: return@getFromLocation

                    val text =
                        formatAddress(address)

                    runOnUiThread {

                        currentAddress = text

                        addressText.text = text
                    }
                }

            } catch (e: Exception) {
                // Geocoder failed
            }

        } else {

            thread {

                try {

                    @Suppress("DEPRECATION")
                    val addresses =
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                    val address =
                        addresses?.firstOrNull()
                            ?: return@thread

                    val text =
                        formatAddress(address)

                    runOnUiThread {

                        currentAddress = text

                        addressText.text = text
                    }

                } catch (e: Exception) {
                    // No network or geocoder failure
                }
            }
        }
    }

    private fun formatAddress(
        address: android.location.Address
    ): String {

        return listOfNotNull(

            address.thoroughfare?.let { street ->

                if (
                    address.subThoroughfare != null
                ) {

                    "$street ${address.subThoroughfare}"

                } else {

                    street
                }
            },

            address.locality
                ?: address.subAdminArea,

            address.countryName

        ).joinToString(", ")
    }

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        if (
            event == null ||
            event.sensor.type != Sensor.TYPE_LIGHT
        ) {
            return
        }

        currentLux =
            event.values[0]

        val label =
            when {

                currentLux < Sighting.NIGHT_LUX ->
                    "Night"

                currentLux < Sighting.DUSK_LUX ->
                    "Dusk"

                currentLux < Sighting.INDOOR_LUX ->
                    "Indoor"

                else ->
                    "Daylight"
            }

        lightText.text =
            String.format(
                Locale.US,
                "%.0f lx · %s",
                currentLux,
                label
            )

        val normalised =
            (
                    log10(currentLux + 1f) /
                            log10(100000f)
                    ) * 100f

        lightBar.progress =
            min(
                100f,
                normalised
            ).toInt()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Not needed
    }

    private fun openGallery() {

        val intent =
            Intent(
                Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).apply {

                type = "image/*"
            }

        photoLauncher.launch(intent)
    }

    /**
     * Hands a camera app an empty file to fill. The file lives in our own
     * cache directory and travels as a content:// URI through FileProvider —
     * a file:// URI has thrown FileUriExposedException since Android 7.
     */
    private fun takePhoto() {

        val captures =
            File(cacheDir, "captures")

        captures.mkdirs()

        val target =
            File(
                captures,
                "${UUID.randomUUID()}.jpg"
            )

        val intent =
            Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            ).apply {

                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    captureUri(target)
                )

                addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

        try {

            pendingCapture = target

            cameraLauncher.launch(intent)

        } catch (e: ActivityNotFoundException) {

            pendingCapture = null

            Snackbar.make(
                saveButton,
                getString(R.string.no_camera_app),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun captureUri(
        file: File
    ): Uri =
        FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )

    private fun saveSighting() {

        val title =
            titleInput
                .text
                .toString()
                .trim()

        if (title.isEmpty()) {

            titleInput.error =
                "Give it a name"

            Snackbar.make(
                saveButton,
                "A sighting needs a title",
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        val attach =
            attachLocationSwitch.isChecked

        val location =
            if (attach) {
                currentLocation
            } else {
                null
            }

        val sighting =
            Sighting(

                id = UUID.randomUUID()
                    .toString(),

                title = title,

                note =
                    noteInput
                        .text
                        .toString()
                        .trim(),

                category =
                    categorySpinner
                        .selectedItem
                        ?.toString()
                        ?: "Other",

                rating =
                    ratingInput.rating,

                timestamp =
                    System.currentTimeMillis(),

                latitude =
                    location?.latitude
                        ?: 0.0,

                longitude =
                    location?.longitude
                        ?: 0.0,

                address =
                    if (attach) {
                        currentAddress
                    } else {
                        ""
                    },

                lux = currentLux,

                photoPath = photoPath
            )

        val data =
            Intent().putExtra(
                MainActivity.EXTRA_SIGHTING,
                sighting
            )

        setResult(
            Activity.RESULT_OK,
            data
        )

        finish()
    }
}
