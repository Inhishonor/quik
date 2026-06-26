/*
 * Copyright (C) 2025 QUIK SMS
 *
 * This file is part of QUIK SMS.
 *
 * QUIK SMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QUIK SMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QUIK SMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.provider.OpenableColumns
import dev.octoshrimpy.quik.model.Attachment
import dev.octoshrimpy.quik.util.ImageUtils.getScaledGif
import timber.log.Timber
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class GifCompressor(
    /**
     * Dampens the amount the target compression amount will influence scale ratio.
     * In order to reduce compression, lower this number, and to increase it, raise this number
     *
     * @property compressionParameter
     */
    val compressionParameter: Double = 0.35,

    /**
     * Determines minimum file dimensions,
     * adjust to avoid large GIFS compressed to unreadable amounts
     */

    val minFileDimensions: Int = 20
) {
    /**
     * GIFs are very expensive to compress, so does a quick compression
     * by estimating the scale down ratio using byte density, and overall size
     * then compressing that way.
     *
     * Can be fine-tuned using [compressionParameter], and currently leans toward overcompression
     */
    fun compressGif(
        context: Context,
        attachment: Attachment,
        maxBytes: Int,
        origWidth: Int,
        aspectRatio: Float
    ): ByteArray {
        // Determine file properties
        val rawFileSize = fetchGifFileSize(context, attachment).coerceAtLeast(1)
        val origHeight = (origWidth / aspectRatio).toInt()
        val totalPixelsPerFrame = origWidth * origHeight
        val byteDensity = max(1.0, rawFileSize.toDouble() / totalPixelsPerFrame)

        // Figure out generally how much we will have to compress,
        // then dampen it and create a base scale
        val targetCompressionRatio = rawFileSize.toDouble() / maxBytes.toDouble()
        val targetCompressionDampened =
            compressionParameter * (compressionParameter * 10) / targetCompressionRatio
        val baseScale = 1.0 / (sqrt(targetCompressionRatio) + targetCompressionDampened)

        // Calculate how much byte density deviates from expected values
        val byteDensityScore = byteDensityScore(byteDensity, rawFileSize.toDouble())

        // Determine how aggressive the compression will be, based mainly on byteDensityScore
        val compressionScale = (targetCompressionRatio / (compressionParameter * 2))
        val compressionAggressiveness =
            targetCompressionRatio.pow(byteDensityScore) / (compressionScale)

        // How much we will have to scale down the GIF, can't be over 1
        val scaleRatio = baseScale.pow(compressionAggressiveness).coerceAtMost(1.0)

        // Determine compressed dimensions from scale ratio
        val midWidthGif = (origWidth * scaleRatio).toInt().coerceAtLeast(minFileDimensions)
        val midHeightGif = (midWidthGif / aspectRatio).toInt().coerceAtLeast(minFileDimensions)

        return getScaledGif(context, attachment.uri, midWidthGif, midHeightGif)
    }

    /**
     * Get the file size of a GIF, by first using contentResolver.openAssetFileDescriptor
     * then reading the size directly from contentResolver in the event it fails.
     */
    private fun fetchGifFileSize(context: Context, attachment: Attachment): Long {
        val resolver = context.contentResolver

        val afdSize = resolver
            .openAssetFileDescriptor(attachment.uri, "r")
            ?.use { it.length }

        if (afdSize != null && afdSize != AssetFileDescriptor.UNKNOWN_LENGTH) {
            return afdSize
        }

        val cursorSize = resolver.query(
            attachment.uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index != -1 && cursor.moveToFirst() && !cursor.isNull(index)) {
                cursor.getLong(index)
            } else {
                null
            }
        }

        if (cursorSize != null && cursorSize > 0) {
            return cursorSize
        }

        val inputStreamBytes = resolver.openInputStream(attachment.uri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }
            totalBytes
        }
        return requireNotNull(inputStreamBytes)
    }

    /**
     * Determines how much byte density deviates from expected.
     * If there is a lot of deviation, we can assume that the GIF is going to require more compression
     */
    private fun byteDensityScore(byteDensity: Double, rawFileSize: Double): Double {
        val d = ln(1.0 + byteDensity)
        val r = ln(1.0 + rawFileSize)

        val t = d / (d + r)
        return 2.0 - 8.0 * t * (1.0 - t)
    }

}
