/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.quest.ui.questionnaire.attachment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * The largest saved photo, in bytes. A visit saves its files inside the QuestionnaireResponse,
 * which must stay under the 10,000,000-byte limit checked at submit. The VBG form has 24 photo
 * items: at this size they take 8,000,000 bytes as base64, which leaves room for a 1 MB PDF and the
 * text.
 */
const val MAX_PHOTO_BYTES = 250_000

/** The largest file that can't be shrunk, such as a PDF, in bytes. */
const val MAX_OTHER_FILE_BYTES = 1_000_000

/**
 * The largest photo read for shrinking, in bytes. A phone photo of 50 megapixels is usually under
 * 20 MB. The file is read into memory whole, so a larger one is refused before it can use up the
 * app's memory.
 */
const val MAX_PHOTO_INPUT_BYTES = 30_000_000

/** The long edges a photo is tried at, largest first, until it fits [MAX_PHOTO_BYTES]. */
private val PHOTO_LONG_EDGES = listOf(1200, 1000, 800, 600)

private const val JPEG_QUALITY = 80

const val JPEG_CONTENT_TYPE = "image/jpeg"

/** A file ready to save as an answer, with its MIME type. */
class SavedFile(val bytes: ByteArray, val contentType: String)

/**
 * Returns the photo to save for a picture the worker added. A picture with a long edge of 1200 px
 * or less and [maxBytes] or less is kept as it is. Any other picture becomes a JPEG: turned as its
 * EXIF orientation says, with white under transparent parts, and with its long edge at 1200 px,
 * then 1000, 800 and 600 px until it fits [maxBytes]. Returns null if Android can't decode the
 * picture or it doesn't fit even at 600 px.
 */
fun shrinkPhoto(
  bytes: ByteArray,
  contentType: String,
  maxBytes: Int = MAX_PHOTO_BYTES,
): SavedFile? {
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
  val longEdge = max(bounds.outWidth, bounds.outHeight)
  if (longEdge <= PHOTO_LONG_EDGES.first() && bytes.size <= maxBytes) {
    return SavedFile(bytes, contentType)
  }

  // Decoding at a power-of-two sample size keeps a 12 MP photo's bitmap near 12 MB, not 48 MB
  val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(longEdge) }
  val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
  val orientation = exifOrientation(bytes)
  try {
    for (edge in PHOTO_LONG_EDGES) {
      val jpeg = encodeJpeg(decoded, edge, orientation)
      if (jpeg.size <= maxBytes) return SavedFile(jpeg, JPEG_CONTENT_TYPE)
    }
    return null
  } finally {
    decoded.recycle()
  }
}

/** The largest power of two that keeps the decoded long edge at 1200 px or more. */
private fun sampleSize(longEdge: Int): Int {
  var sampleSize = 1
  while (longEdge / (sampleSize * 2) >= PHOTO_LONG_EDGES.first()) sampleSize *= 2
  return sampleSize
}

private fun encodeJpeg(decoded: Bitmap, longEdge: Int, orientation: Int): ByteArray {
  val scale = min(1f, longEdge.toFloat() / max(decoded.width, decoded.height))
  val matrix =
    Matrix().apply {
      postScale(scale, scale)
      turnUpright(orientation)
    }
  val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
  // JPEG has no transparency: without a background, transparent parts turn black
  val opaque =
    if (transformed.hasAlpha()) {
      Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888).also {
        Canvas(it).apply {
          drawColor(Color.WHITE)
          drawBitmap(transformed, 0f, 0f, null)
        }
      }
    } else {
      transformed
    }
  return try {
    ByteArrayOutputStream().use { output ->
      opaque.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
      output.toByteArray()
    }
  } finally {
    if (opaque !== transformed) opaque.recycle()
    if (transformed !== decoded) transformed.recycle()
  }
}

/**
 * The picture's EXIF orientation. Saving it as a new JPEG drops the EXIF, so the turn is applied.
 */
private fun exifOrientation(bytes: ByteArray): Int =
  try {
    ExifInterface(ByteArrayInputStream(bytes))
      .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
  } catch (exception: Exception) {
    ExifInterface.ORIENTATION_NORMAL
  }

/** Turns and flips the picture as its EXIF [orientation] says, so it shows upright. */
private fun Matrix.turnUpright(orientation: Int) {
  when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
    ExifInterface.ORIENTATION_TRANSPOSE -> {
      postRotate(90f)
      postScale(-1f, 1f)
    }
    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
    ExifInterface.ORIENTATION_TRANSVERSE -> {
      postRotate(-90f)
      postScale(-1f, 1f)
    }
    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
  }
}
