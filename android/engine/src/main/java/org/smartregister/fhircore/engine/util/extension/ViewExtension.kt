package org.smartregister.fhircore.engine.util.extension

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat

fun View.show() {
  this.visibility = View.VISIBLE
}

fun View.hide() {
  this.visibility = View.GONE
}

fun View.toggleVisibility(show: Boolean) =
  if (show) this.visibility = View.VISIBLE else this.visibility = View.GONE

enum class DrawablePosition(val position: Int) {
  DRAWABLE_LEFT(0),
  DRAWABLE_TOP(1),
  DRAWABLE_RIGHT(2),
  DRAWABLE_BOTTOM(3)
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.addOnDrawableClickListener(drawablePosition: DrawablePosition, onClicked: () -> Unit) {
  this.setOnTouchListener(
    object : View.OnTouchListener {
      override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        if (motionEvent == null || view == null) return false
        if (motionEvent.action == MotionEvent.ACTION_UP &&
            (view as EditText).isDrawableClicked(drawablePosition, motionEvent)
        ) {
          onClicked()
          return true
        }
        return false
      }
    }
  )
}

private fun EditText.isDrawableClicked(
  drawablePosition: DrawablePosition,
  motionEvent: MotionEvent?,
): Boolean {
  if (motionEvent == null) return false
  return when (drawablePosition) {
    DrawablePosition.DRAWABLE_RIGHT ->
      motionEvent.rawX >=
        (this.right - this.compoundDrawables[drawablePosition.position].bounds.width())
    DrawablePosition.DRAWABLE_LEFT ->
      motionEvent.rawX <= (this.compoundDrawables[drawablePosition.position].bounds.width())
    DrawablePosition.DRAWABLE_TOP, DrawablePosition.DRAWABLE_BOTTOM -> return false
  }
}

fun View.getDrawable(drawableResourceId: Int): Drawable? =
  ContextCompat.getDrawable(context, drawableResourceId)
