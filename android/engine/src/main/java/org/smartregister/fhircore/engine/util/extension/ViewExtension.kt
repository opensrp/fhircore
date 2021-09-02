/*
 * Copyright 2021 Ona Systems, Inc
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

fun View.hide(gone: Boolean = true) {
  if (gone) this.visibility = View.GONE else this.visibility = View.INVISIBLE
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
