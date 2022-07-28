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

package org.smartregister.fhircore.engine.ui.bottomsheet

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import io.mockk.spyk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric.buildActivity
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class RegisterBottomSheetTest : RobolectricTest() {

  private val registerBottomSheet =
    spyk(
      RegisterBottomSheet(
        listOf(
          NavigationMenuConfig(id = "UniqueTag1", display = "Menu 1"),
          NavigationMenuConfig(id = "UniqueTag2", display = "Menu 2")
        )
      ) {}
    )

  private lateinit var activity: AppCompatActivity

  @Before
  fun setUp() {
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    activity = buildActivity(AppCompatActivity::class.java).create().resume().get()
  }

  @After
  fun tearDown() {
    activity.finish()
  }

  @Test
  fun testThatBottomSheetIsShown() {
    Assert.assertEquals(2, registerBottomSheet.navigationMenuConfigs?.size)
    registerBottomSheet.show(activity.supportFragmentManager, RegisterBottomSheet.TAG)
    Assert.assertTrue(registerBottomSheet.showsDialog)
  }
}
