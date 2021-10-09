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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class AndroidExtensionTest : RobolectricTest() {
  private lateinit var context: Application

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Application>()
  }

  @Test
  fun testGetDrawableShouldReturnDefaultIfInvalidNameSpecified() {
    val result = context.getDrawable("invalid")

    val expected = context.getDrawable(R.drawable.ic_default_logo)!!
    assertEquals(expected.constantState, result.constantState)
  }

  @Test
  fun testGetDrawableShouldReturnCorrectDrawable() {
    val result = context.getDrawable("ic_menu")

    val expected = context.getDrawable(R.drawable.ic_menu)!!
    assertEquals(expected.constantState, result.constantState)
  }

  @Test
  fun testGetThemeShouldReturnDefaultIfInvalidNameSpecified() {
    val result = context.getTheme("invalid")

    val expected = R.style.AppTheme_NoActionBar
    assertEquals(R.style.AppTheme_NoActionBar, result)
  }

  @Test
  fun testGetDrawableShouldReturnCorrectTheme() {
    val result = context.getTheme("AppTheme.PopupOverlay")

    assertEquals(R.style.AppTheme_PopupOverlay, result)
  }
}
