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

import android.graphics.Rect
import android.graphics.drawable.VectorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class ViewExtensionTest : RobolectricTest() {

  @Test
  fun `View#show() should change visibility to VISIBLE`() {
    val view = View(ApplicationProvider.getApplicationContext())
    view.visibility = View.GONE

    view.show()

    Assert.assertEquals(View.VISIBLE, view.visibility)
  }

  @Test
  fun `View#hide() should change visibility to GONE when param is true`() {
    val view = View(ApplicationProvider.getApplicationContext())
    view.visibility = View.VISIBLE

    view.hide(true)

    Assert.assertEquals(View.GONE, view.visibility)
  }

  @Test
  fun `View#hide() should change visibility to INVISIBLE when param is false`() {
    val view = View(ApplicationProvider.getApplicationContext())
    view.visibility = View.VISIBLE

    view.hide(false)

    Assert.assertEquals(View.INVISIBLE, view.visibility)
  }

  @Test
  fun `View#toggleVisibility() should change visibility to VISIBLE when param is true`() {
    val view = View(ApplicationProvider.getApplicationContext())
    view.visibility = View.GONE

    view.toggleVisibility(true)

    Assert.assertEquals(View.VISIBLE, view.visibility)
  }

  @Test
  fun `View#toggleVisibility() should change visibility to GONE when param is false`() {
    val view = View(ApplicationProvider.getApplicationContext())
    view.visibility = View.VISIBLE

    view.toggleVisibility(false)

    Assert.assertEquals(View.GONE, view.visibility)
  }

  @Test
  fun `View#getDrawable() should call ContextCompat#getDrawable`() {
    mockkStatic(ContextCompat::getDrawable)
    val view = View(ApplicationProvider.getApplicationContext())

    Assert.assertNotNull(view.getDrawable(R.drawable.camera_flash))

    verify { ContextCompat.getDrawable(view.context, R.drawable.camera_flash) }
    unmockkStatic(ContextCompat::getDrawable)
  }

  @Test
  fun `EditText#isDrawableClicked() should return false when DrawablePosition is DrawableRight`() {
    val editText = EditText(ApplicationProvider.getApplicationContext())
    val drawable = VectorDrawable()
    drawable.bounds = Rect(0, 0, 20, 5)
    editText.setCompoundDrawables(null, null, drawable, null)
    val mouseEvent = MotionEvent.obtain(5L, 5L, 10, -25F, -5F, 0)

    Assert.assertFalse(
      ReflectionHelpers.callStaticMethod(
        Class.forName("org.smartregister.fhircore.engine.util.extension.ViewExtensionKt"),
        "isDrawableClicked",
        ReflectionHelpers.ClassParameter.from(EditText::class.java, editText),
        ReflectionHelpers.ClassParameter.from(
          DrawablePosition::class.java,
          DrawablePosition.DRAWABLE_RIGHT
        ),
        ReflectionHelpers.ClassParameter.from(MotionEvent::class.java, mouseEvent)
      )
    )
  }

  @Test
  fun `EditText#isDrawableClicked() should return false when DrawablePosition is DrawableLeft`() {
    val editText = EditText(ApplicationProvider.getApplicationContext())
    val drawable = VectorDrawable()
    drawable.bounds = Rect(0, 0, 20, 5)
    editText.setCompoundDrawables(drawable, null, null, null)
    val mouseEvent = MotionEvent.obtain(5L, 5L, 10, 25F, 5F, 0)

    Assert.assertFalse(
      ReflectionHelpers.callStaticMethod(
        Class.forName("org.smartregister.fhircore.engine.util.extension.ViewExtensionKt"),
        "isDrawableClicked",
        ReflectionHelpers.ClassParameter.from(EditText::class.java, editText),
        ReflectionHelpers.ClassParameter.from(
          DrawablePosition::class.java,
          DrawablePosition.DRAWABLE_LEFT
        ),
        ReflectionHelpers.ClassParameter.from(MotionEvent::class.java, mouseEvent)
      )
    )
  }

  @Test
  fun `EditText#isDrawableClicked() should return true when DrawablePosition is DrawableRight`() {
    val editText = EditText(ApplicationProvider.getApplicationContext())
    val drawable = VectorDrawable()
    drawable.bounds = Rect(0, 0, 20, 5)
    editText.setCompoundDrawables(null, null, drawable, null)
    val mouseEvent = MotionEvent.obtain(5L, 5L, 10, 5F, 5F, 0)

    Assert.assertTrue(
      ReflectionHelpers.callStaticMethod(
        Class.forName("org.smartregister.fhircore.engine.util.extension.ViewExtensionKt"),
        "isDrawableClicked",
        ReflectionHelpers.ClassParameter.from(EditText::class.java, editText),
        ReflectionHelpers.ClassParameter.from(
          DrawablePosition::class.java,
          DrawablePosition.DRAWABLE_RIGHT
        ),
        ReflectionHelpers.ClassParameter.from(MotionEvent::class.java, mouseEvent)
      )
    )
  }

  @Test
  fun `EditText#isDrawableClicked() should return true when DrawablePosition is DrawableLeft`() {
    val editText = EditText(ApplicationProvider.getApplicationContext())
    val drawable = VectorDrawable()
    drawable.bounds = Rect(0, 0, 20, 5)
    editText.setCompoundDrawables(drawable, null, null, null)
    val mouseEvent = MotionEvent.obtain(5L, 5L, 10, 5F, 5F, 0)

    Assert.assertTrue(
      ReflectionHelpers.callStaticMethod(
        Class.forName("org.smartregister.fhircore.engine.util.extension.ViewExtensionKt"),
        "isDrawableClicked",
        ReflectionHelpers.ClassParameter.from(EditText::class.java, editText),
        ReflectionHelpers.ClassParameter.from(
          DrawablePosition::class.java,
          DrawablePosition.DRAWABLE_LEFT
        ),
        ReflectionHelpers.ClassParameter.from(MotionEvent::class.java, mouseEvent)
      )
    )
  }

  @Test
  fun `EditText#addOnDrawableClickListener() should setOnTouchListener`() {
    val editText = spyk(EditText(ApplicationProvider.getApplicationContext()))
    val onTouchListenerSlot = slot<View.OnTouchListener>()

    val onClicked = {}
    editText.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT, onClicked)

    verify { editText.setOnTouchListener(capture(onTouchListenerSlot)) }

    Assert.assertNotNull(onTouchListenerSlot.captured)
  }

  @Test
  fun `EditText#addOnDrawableClickListener() onTouchListener should return false when motionEvent is null`() {
    val editText = spyk(EditText(ApplicationProvider.getApplicationContext()))
    val onTouchListenerSlot = slot<View.OnTouchListener>()

    val onClicked = {}
    editText.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT, onClicked)

    verify { editText.setOnTouchListener(capture(onTouchListenerSlot)) }
    Assert.assertFalse(onTouchListenerSlot.captured.onTouch(editText, null))
  }

  @Test
  fun `EditText#addOnDrawableClickListener() onTouchListener should return false when view is null`() {
    val editText = spyk(EditText(ApplicationProvider.getApplicationContext()))
    val onTouchListenerSlot = slot<View.OnTouchListener>()

    val onClicked = {}
    editText.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT, onClicked)

    verify { editText.setOnTouchListener(capture(onTouchListenerSlot)) }
    Assert.assertFalse(onTouchListenerSlot.captured.onTouch(null, mockk()))
  }

  @Test
  fun `EditText#addOnDrawableClickListener() onTouchListener should return false when motionEvent Action is ACTION_DOWN`() {
    val editText = spyk(EditText(ApplicationProvider.getApplicationContext()))
    val onTouchListenerSlot = slot<View.OnTouchListener>()
    val motionEvent = MotionEvent.obtain(5L, 5L, 10, 5F, 5F, 0)
    motionEvent.action = MotionEvent.ACTION_DOWN

    val onClicked = {}
    editText.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT, onClicked)

    verify { editText.setOnTouchListener(capture(onTouchListenerSlot)) }
    Assert.assertFalse(onTouchListenerSlot.captured.onTouch(null, motionEvent))
  }

  @Test
  fun `EditText#addOnDrawableClickListener() onTouchListener should return true and call onClick`() {
    val editText = spyk(EditText(ApplicationProvider.getApplicationContext()))
    val drawable = VectorDrawable()
    drawable.bounds = Rect(0, 0, 20, 5)
    every { editText.compoundDrawables } returns arrayOf(null, null, drawable, null)
    val onTouchListenerSlot = slot<View.OnTouchListener>()
    val motionEvent = MotionEvent.obtain(5L, 5L, 10, 5F, 5F, 0)
    motionEvent.action = MotionEvent.ACTION_UP

    val onClicked = spyk({})
    editText.addOnDrawableClickListener(DrawablePosition.DRAWABLE_RIGHT, onClicked)

    verify { editText.setOnTouchListener(capture(onTouchListenerSlot)) }
    Assert.assertTrue(onTouchListenerSlot.captured.onTouch(editText, motionEvent))

    verify { onClicked.invoke() }
  }
}
