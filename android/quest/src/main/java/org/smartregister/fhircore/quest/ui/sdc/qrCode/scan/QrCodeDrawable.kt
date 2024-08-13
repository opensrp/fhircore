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

package org.smartregister.fhircore.quest.ui.sdc.qrCode.scan

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.barcode.common.Barcode

internal class QrCodeDrawable(barcode: Barcode) : Drawable() {
  private val boundingRectPaint =
    Paint().apply {
      style = Paint.Style.STROKE
      color = Color.YELLOW
      strokeWidth = 5F
      alpha = 200
    }

  private val boundingRect = barcode.boundingBox!!

  override fun draw(canvas: Canvas) {
    canvas.drawRect(boundingRect, boundingRectPaint)
  }

  override fun setAlpha(alpha: Int) {
    boundingRectPaint.alpha = alpha
  }

  override fun setColorFilter(colorFiter: ColorFilter?) {
    boundingRectPaint.colorFilter = colorFilter
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"),
  )
  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
