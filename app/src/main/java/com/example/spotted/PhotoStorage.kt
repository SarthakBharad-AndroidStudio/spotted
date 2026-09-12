package com.example.spotted

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies picked images into app-internal storage so they survive the
 * content-URI permission grant expiring, and decodes them scaled down.
 */
object PhotoStorage {

    private const val DIR = "photos"

    private fun dir(context: Context): File =
        File(
            context.filesDir,
            DIR
        ).apply {
            if (!exists()) {
                mkdirs()
            }
        }

    /**
     * Returns the absolute path of the stored copy,
     * or null if the copy failed.
     */
    fun store(
        context: Context,
        source: Uri
    ): String? {

        return try {

            val target = File(
                dir(context),
                "${UUID.randomUUID()}.jpg"
            )

            context.contentResolver
                .openInputStream(source)
                ?.use { input ->

                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }

                }
                ?: return null

            target.absolutePath

        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {

        if (path.isNullOrBlank()) {
            return
        }

        try {
            File(path)
                .takeIf { it.exists() }
                ?.delete()

        } catch (e: Exception) {
            // Nothing useful to do.
        }
    }

    /**
     * Decode at roughly the size we're going to display.
     */
    fun decodeScaled(
        path: String?,
        reqWidth: Int
    ): Bitmap? {

        if (path.isNullOrBlank()) {
            return null
        }

        val file = File(path)

        if (!file.exists()) {
            return null
        }

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        BitmapFactory.decodeFile(
            path,
            bounds
        )

        var sample = 1

        while (
            bounds.outWidth / sample >
            reqWidth * 2
        ) {
            sample *= 2
        }

        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sample
            }

        return BitmapFactory.decodeFile(
            path,
            options
        )
    }
}