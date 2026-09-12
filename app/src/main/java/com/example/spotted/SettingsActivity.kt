package com.example.spotted

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Straight SharedPreferences round trip.
 */
class SettingsActivity :
    AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_settings
        )

        val toolbar: MaterialToolbar =
            findViewById(
                R.id.settingsToolbar
            )

        setSupportActionBar(
            toolbar
        )

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val goalLabel: TextView =
            findViewById(
                R.id.goalLabel
            )

        val goalSeek: SeekBar =
            findViewById(
                R.id.goalSeek
            )

        val attachSwitch: MaterialSwitch =
            findViewById(
                R.id.attachSwitch
            )

        val addressSwitch: MaterialSwitch =
            findViewById(
                R.id.addressSwitch
            )

        val saveButton: Button =
            findViewById(
                R.id.settingsSave
            )

        // SeekBar is 0-based, goal is 1-based.

        val savedGoal =
            SightingStore.getGoal(
                this
            )

        goalSeek.progress =
            savedGoal - 1

        goalLabel.text =
            "Weekly goal: $savedGoal sightings"

        attachSwitch.isChecked =
            SightingStore.isAttachLocation(
                this
            )

        addressSwitch.isChecked =
            SightingStore.isShowAddress(
                this
            )

        goalSeek.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    bar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    goalLabel.text =
                        "Weekly goal: ${progress + 1} sightings"
                }

                override fun onStartTrackingTouch(
                    bar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    bar: SeekBar?
                ) {
                }
            }
        )

        saveButton.setOnClickListener {

            SightingStore.setGoal(
                this,
                goalSeek.progress + 1
            )

            SightingStore.setAttachLocation(
                this,
                attachSwitch.isChecked
            )

            SightingStore.setShowAddress(
                this,
                addressSwitch.isChecked
            )

            setResult(
                Activity.RESULT_OK
            )

            finish()
        }
    }
}