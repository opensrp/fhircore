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

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Random
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoShrinkerTest {

  @Test
  fun testLargePhotoBecomesAJpegWithALongEdgeOf1200Pixels() {
    val photo = encode(gradient(3000, 2000), Bitmap.CompressFormat.JPEG)

    val saved = shrinkPhoto(photo, "image/jpeg")!!

    Assert.assertEquals("image/jpeg", saved.contentType)
    Assert.assertTrue(saved.bytes.size <= MAX_PHOTO_BYTES)
    Assert.assertEquals(1200 to 800, size(saved.bytes))
  }

  @Test
  fun testPhotoTooLargeAt1200PixelsStepsDownTo1000Pixels() {
    val photo = encode(noise(3000, 2000), Bitmap.CompressFormat.JPEG)
    val at1200Pixels = shrinkPhoto(photo, "image/jpeg", maxBytes = Int.MAX_VALUE)!!
    Assert.assertEquals(1200 to 800, size(at1200Pixels.bytes))

    val saved = shrinkPhoto(photo, "image/jpeg", maxBytes = at1200Pixels.bytes.size - 1)!!

    Assert.assertEquals(1000 to 667, size(saved.bytes))
    Assert.assertTrue(saved.bytes.size < at1200Pixels.bytes.size)
  }

  @Test
  fun testPhotoThatDoesNotFitEvenAt600PixelsIsRefused() {
    val photo = encode(noise(3000, 2000), Bitmap.CompressFormat.JPEG)

    Assert.assertNull(shrinkPhoto(photo, "image/jpeg", maxBytes = 100))
  }

  @Test
  fun testSmallPhotoIsKeptAsItIs() {
    val photo = encode(gradient(800, 600), Bitmap.CompressFormat.PNG)

    val saved = shrinkPhoto(photo, "image/png")!!

    Assert.assertSame(photo, saved.bytes)
    Assert.assertEquals("image/png", saved.contentType)
  }

  // The gradient is red on the right and green at the bottom, so a corner's colour shows where the
  // picture's corners went

  @Test
  fun testPhotoIsTurnedAsItsExifOrientationSays() {
    val photo = withOrientation(gradient(3000, 2000), ExifInterface.ORIENTATION_ROTATE_90)

    val saved = shrinkPhoto(photo, "image/jpeg")!!

    Assert.assertEquals(800 to 1200, size(saved.bytes))
    // Turned clockwise: the bottom-left corner goes to the top left
    assertCorner(saved.bytes, 10, 10, red = false, green = true)
  }

  @Test
  fun testTransposedPhotoIsFlippedAndTurned() {
    val photo = withOrientation(gradient(3000, 2000), ExifInterface.ORIENTATION_TRANSPOSE)

    val saved = shrinkPhoto(photo, "image/jpeg")!!

    Assert.assertEquals(800 to 1200, size(saved.bytes))
    // Mirrored along the top-left to bottom-right diagonal: the top-right corner goes to the
    // bottom left
    assertCorner(saved.bytes, 10, 1190, red = true, green = false)
  }

  @Test
  fun testMirroredPhotoIsFlipped() {
    val photo = withOrientation(gradient(3000, 2000), ExifInterface.ORIENTATION_FLIP_HORIZONTAL)

    val saved = shrinkPhoto(photo, "image/jpeg")!!

    Assert.assertEquals(1200 to 800, size(saved.bytes))
    // The top-right corner goes to the top left
    assertCorner(saved.bytes, 10, 10, red = true, green = false)
  }

  @Test
  fun testTransparentPartsTurnWhite() {
    val transparent = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)
    val photo = encode(transparent, Bitmap.CompressFormat.PNG)

    val saved = shrinkPhoto(photo, "image/png")!!

    Assert.assertEquals("image/jpeg", saved.contentType)
    val pixel = BitmapFactory.decodeByteArray(saved.bytes, 0, saved.bytes.size).getPixel(600, 300)
    Assert.assertTrue(Color.red(pixel) > 240 && Color.green(pixel) > 240 && Color.blue(pixel) > 240)
  }

  @Test
  fun testFileThatIsNotAPictureIsRefused() {
    Assert.assertNull(shrinkPhoto("%PDF-1.4 TEST".toByteArray(), "image/jpeg"))
  }

  private fun gradient(width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      val row = IntArray(width)
      for (y in 0 until height) {
        for (x in 0 until width) row[x] = Color.rgb(x * 255 / width, y * 255 / height, 128)
        setPixels(row, 0, width, 0, y, width, 1)
      }
    }

  /** Random pixels, which JPEG can't compress much. */
  private fun noise(width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      val random = Random(1)
      val row = IntArray(width)
      for (y in 0 until height) {
        for (x in 0 until width) row[x] = random.nextInt() or 0xFF000000.toInt()
        setPixels(row, 0, width, 0, y, width, 1)
      }
    }

  private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray =
    ByteArrayOutputStream().use {
      bitmap.compress(format, 90, it)
      it.toByteArray()
    }

  /** A JPEG of [bitmap] with its EXIF orientation set to [orientation]. */
  private fun withOrientation(bitmap: Bitmap, orientation: Int): ByteArray {
    val file = File.createTempFile("TEST_", ".jpg")
    try {
      file.writeBytes(encode(bitmap, Bitmap.CompressFormat.JPEG))
      ExifInterface(file.path).apply {
        setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        saveAttributes()
      }
      return file.readBytes()
    } finally {
      file.delete()
    }
  }

  private fun assertCorner(bytes: ByteArray, x: Int, y: Int, red: Boolean, green: Boolean) {
    val pixel = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).getPixel(x, y)
    Assert.assertEquals("red at $x,$y", red, Color.red(pixel) > 200)
    Assert.assertEquals("green at $x,$y", green, Color.green(pixel) > 200)
  }

  private fun size(bytes: ByteArray): Pair<Int, Int> {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    return bounds.outWidth to bounds.outHeight
  }
}
