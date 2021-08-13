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

package org.smartregister.fhircore.eir.util.extension.view

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import org.smartregister.fhircore.eir.util.Utils

fun View.show() {
  this.visibility = View.VISIBLE
}

fun View.hide() {
  this.visibility = View.GONE
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.addOnDrawableClickedListener(
  drawablePosition: Utils.DrawablePosition,
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
  drawablePosition: Utils.DrawablePosition,
  event: MotionEvent?,
): Boolean {
  return when (drawablePosition) {
    Utils.DrawablePosition.DRAWABLE_RIGHT ->
      event!!.rawX >=
        (this.right - this.compoundDrawables[drawablePosition.position].bounds.width())
    Utils.DrawablePosition.DRAWABLE_LEFT ->
      event!!.rawX <= (this.compoundDrawables[drawablePosition.position].bounds.width())
    else -> {
      return false
    }
  }
}

fun View.setBgColor(color: Int) {
  this.setBackgroundColor(ContextCompat.getColor(this.context, color))
}
