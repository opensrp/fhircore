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

package org.smartregister.fhircore.engine.util.extension

import android.view.View
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
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

    Assert.assertNotNull(
      view.getDrawable(
        com.google.android.fhir.datacapture.contrib.views.barcode.R.drawable.camera_flash,
      ),
    )

    verify {
      ContextCompat.getDrawable(
        view.context,
        com.google.android.fhir.datacapture.contrib.views.barcode.R.drawable.camera_flash,
      )
    }
    unmockkStatic(ContextCompat::getDrawable)
  }
}
