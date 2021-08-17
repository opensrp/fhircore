package org.smartregister.fhircore.engine.util.extension

import android.annotation.SuppressLint
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

fun View.setBgColor(color: Int) {
  this.setBackgroundColor(ContextCompat.getColor(this.context, color))
}

enum class DrawablePosition(val position: Int) {
  DRAWABLE_LEFT(0),
  DRAWABLE_TOP(1),
  DRAWABLE_RIGHT(2),
  DRAWABLE_BOTTOM(3)
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.addOnDrawableClickedListener(
  drawablePosition: DrawablePosition,
  onClicked: () -> Unit
) {
  this.setOnTouchListener(
    object : View.OnTouchListener {
      override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_UP &&
            (v as EditText).isDrawableClicked(drawablePosition, event)
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
  event: MotionEvent?,
): Boolean {
  return when (drawablePosition) {
    DrawablePosition.DRAWABLE_RIGHT ->
      event!!.rawX >=
        (this.right - this.compoundDrawables[drawablePosition.position].bounds.width())
    DrawablePosition.DRAWABLE_LEFT ->
      event!!.rawX <= (this.compoundDrawables[drawablePosition.position].bounds.width())
    else -> {
      return false
    }
  }
}
