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

package org.smartregister.fhircore.engine.navigation

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
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem

class NavigationBottomSheetTest : RobolectricTest() {

  private val navigationBottomSheet = spyk(NavigationBottomSheet {})

  private lateinit var activity: AppCompatActivity

  private val registerItems =
    listOf(
      RegisterItem(uniqueTag = "UniqueTag1", title = "Menu 1", isSelected = true),
      RegisterItem(uniqueTag = "UniqueTag2", title = "Menu 2", isSelected = false)
    )

  @Before
  fun setUp() {
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
    activity = buildActivity(AppCompatActivity::class.java).create().resume().get()
    navigationBottomSheet.registersList = registerItems
  }

  @After
  fun tearDown() {
    activity.finish()
  }

  @Test
  fun testThatBottomSheetIsShown() {
    Assert.assertEquals(2, navigationBottomSheet.registersList.size)
    navigationBottomSheet.show(activity.supportFragmentManager, NavigationBottomSheet.TAG)
    Assert.assertTrue(navigationBottomSheet.showsDialog)
  }
}
