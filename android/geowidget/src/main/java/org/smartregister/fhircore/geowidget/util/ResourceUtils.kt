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

package org.smartregister.fhircore.geowidget.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import timber.log.Timber

object ResourceUtils {

  fun drawableToBitmap(drawable: Drawable): Bitmap? {
    try {
      if (drawable is BitmapDrawable) {
        return drawable.bitmap
      }
      val bitmap =
        Bitmap.createBitmap(
          drawable.intrinsicWidth,
          drawable.intrinsicHeight,
          Bitmap.Config.ARGB_8888,
        )
      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      return bitmap
    } catch (e: Exception) {
      Timber.e(e)
      return null
    }
  }
}
