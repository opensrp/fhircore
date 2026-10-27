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

package org.smartregister.fhircore.quest.ui.questionnaire.signature

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import java.io.ByteArrayOutputStream
import org.smartregister.fhircore.quest.R

class SignatureCaptureView
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

  private val drawingView = DrawingCanvasView(context)
  private lateinit var clearButton: Button
  private lateinit var primaryButton: Button
  private var onSignatureChangedListener: ((Boolean) -> Unit)? = null
  private var suppressListener = false
  private var isLocked = false

  init {
    orientation = VERTICAL

    clearButton =
      Button(context).apply {
        text = context.getString(R.string.signature_clear_button)
        backgroundTintList = ColorStateList.valueOf(CLEAR_BUTTON_BLUE)
        isEnabled = false
        alpha = DISABLED_ALPHA
        setOnClickListener { clearByUser() }
      }

    primaryButton =
      Button(context).apply {
        text = context.getString(R.string.signature_done_button)
        backgroundTintList = ColorStateList.valueOf(DONE_BUTTON_GREEN)
        isEnabled = false
        alpha = DISABLED_ALPHA
        setOnClickListener { onPrimaryClicked() }
      }

    val canvasParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(CANVAS_HEIGHT_DP))
    addView(drawingView, canvasParams)

    val buttonRow =
      LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.END
      }

    val clearParams =
      LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        topMargin = dpToPx(8)
        marginEnd = dpToPx(8)
      }
    buttonRow.addView(clearButton, clearParams)

    val primaryParams =
      LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        topMargin = dpToPx(8)
      }
    buttonRow.addView(primaryButton, primaryParams)

    addView(buttonRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

    drawingView.onStrokeDrawn = { notifyChanged(true) }
    updateUi()
  }

  fun setOnSignatureChangedListener(listener: (hasSignature: Boolean) -> Unit) {
    onSignatureChangedListener = listener
  }

  fun isEmpty(): Boolean = drawingView.isEmpty()

  fun isLocked(): Boolean = isLocked

  fun clearByUser() {
    if (isLocked) return
    drawingView.clear()
    notifyChanged(false)
  }

  fun clearSilently() {
    suppressListener = true
    try {
      isLocked = false
      drawingView.clear()
      updateUi()
    } finally {
      suppressListener = false
    }
  }

  fun getSignaturePngBase64(): String? {
    if (drawingView.isEmpty()) return null
    val bitmap = drawingView.exportBitmap()
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
  }

  fun loadSignaturePngBase64(base64: String) {
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
    suppressListener = true
    try {
      isLocked = true
      drawingView.loadBitmap(bitmap)
      updateUi()
    } finally {
      suppressListener = false
    }
  }

  private fun onPrimaryClicked() {
    if (isLocked) {
      isLocked = false
      updateUi()
    } else {
      if (drawingView.isEmpty()) return
      isLocked = true
      updateUi()
    }
  }

  private fun notifyChanged(hasSignature: Boolean) {
    updateUi()
    if (!suppressListener) onSignatureChangedListener?.invoke(hasSignature)
  }

  private fun updateUi() {
    val hasContent = !drawingView.isEmpty()
    drawingView.setLocked(isLocked)

    if (isLocked) {
      clearButton.visibility = View.GONE
      primaryButton.text = context.getString(R.string.signature_edit_button)
      primaryButton.backgroundTintList = ColorStateList.valueOf(EDIT_BUTTON_GREY)
      primaryButton.isEnabled = true
      primaryButton.alpha = 1f
    } else {
      clearButton.visibility = View.VISIBLE
      clearButton.isEnabled = hasContent
      clearButton.alpha = if (hasContent) 1f else DISABLED_ALPHA

      primaryButton.text = context.getString(R.string.signature_done_button)
      primaryButton.backgroundTintList = ColorStateList.valueOf(DONE_BUTTON_GREEN)
      primaryButton.isEnabled = hasContent
      primaryButton.alpha = if (hasContent) 1f else DISABLED_ALPHA
    }
  }

  private fun dpToPx(dp: Int): Int =
    (dp * context.resources.displayMetrics.density).toInt()

  private class DrawingCanvasView(context: Context) : View(context) {

    var onStrokeDrawn: (() -> Unit)? = null

    private var locked = false

    private val paint =
      Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
      }

    private val backgroundPaint =
      Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
      }

    private val borderPaint =
      Paint().apply {
        color = BORDER_GREY
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1).toFloat()
        isAntiAlias = true
      }

    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var hasContent = false
    private var loadedBitmap: Bitmap? = null

    init {
      background =
        GradientDrawable().apply {
          setColor(Color.parseColor("#FAFAFA"))
          cornerRadius = dpToPx(4).toFloat()
        }
    }

    fun isEmpty(): Boolean = !hasContent

    fun setLocked(value: Boolean) {
      if (locked == value) return
      locked = value
      borderPaint.color = if (value) BORDER_LOCKED_GREEN else BORDER_GREY
      invalidate()
    }

    fun clear() {
      path.reset()
      loadedBitmap = null
      hasContent = false
      invalidate()
    }

    fun exportBitmap(): Bitmap {
      val w = width.coerceAtLeast(1)
      val h = height.coerceAtLeast(1)

      val targetW = (w / 2).coerceAtLeast(1)
      val targetH = (h / 2).coerceAtLeast(1)

      val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
      val canvas = Canvas(bitmap)
      canvas.scale(0.5f, 0.5f)
      canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)
      loadedBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
      canvas.drawPath(path, paint)
      return bitmap
    }

    fun loadBitmap(bitmap: Bitmap) {
      loadedBitmap = bitmap
      hasContent = true
      invalidate()
    }

    override fun onDraw(canvas: Canvas) {
      super.onDraw(canvas)
      loadedBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
      canvas.drawPath(path, paint)
      val half = borderPaint.strokeWidth / 2f
      val radius = dpToPx(4).toFloat()
      canvas.drawRoundRect(
        half,
        half,
        width - half,
        height - half,
        radius,
        radius,
        borderPaint,
      )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
      if (locked) return false
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          parent?.requestDisallowInterceptTouchEvent(true)
          path.moveTo(event.x, event.y)
          path.lineTo(event.x, event.y)
          lastX = event.x
          lastY = event.y
          hasContent = true
          onStrokeDrawn?.invoke()
        }
        MotionEvent.ACTION_MOVE -> {
          val midX = (lastX + event.x) / 2
          val midY = (lastY + event.y) / 2
          path.quadTo(lastX, lastY, midX, midY)
          lastX = event.x
          lastY = event.y
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          parent?.requestDisallowInterceptTouchEvent(false)
          onStrokeDrawn?.invoke()
        }
        else -> return false
      }
      invalidate()
      return true
    }

    private fun dpToPx(dp: Int): Int =
      (dp * resources.displayMetrics.density).toInt()
  }

  companion object {
    private val CLEAR_BUTTON_BLUE = Color.parseColor("#1976D2")
    private val DONE_BUTTON_GREEN = Color.parseColor("#2E7D32")
    private val EDIT_BUTTON_GREY = Color.parseColor("#616161")
    private val BORDER_GREY = Color.parseColor("#9E9E9E")
    private val BORDER_LOCKED_GREEN = Color.parseColor("#2E7D32")
    private const val DISABLED_ALPHA = 0.5f
    private const val CANVAS_HEIGHT_DP = 180
  }
}
