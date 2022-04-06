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

package org.smartregister.fhircore.anc.util

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.spyk
import org.junit.Before
import org.junit.Rule
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetDataModel
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetHolder
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetListView
import org.smartregister.fhircore.anc.util.bottomsheet.OnClickedListItems

class BottomSheetListViewTest : RobolectricTest() {

  private val onClickedListItems: OnClickedListItems = spyk()

  @get:Rule val composeRule = createComposeRule()

  private lateinit var bottomSheetHolder: BottomSheetHolder

  private lateinit var activity: AppCompatActivity

  @Before
  fun setUp() {
    ApplicationProvider.getApplicationContext<Context>().apply {
      setTheme(org.smartregister.fhircore.engine.R.style.AppTheme)
    }
    activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().resume().get()
    bottomSheetHolder =
      BottomSheetHolder(
        activity.getString(R.string.label_assign_new_family_head),
        activity.getString(R.string.label_select_new_head),
        activity.getString(R.string.label_remove_family_warning),
        listOf(
          BottomSheetDataModel("TestFragmentTag", "All Clients", "1241", true),
          BottomSheetDataModel("TestFragmentTag", "All Clients", "1241")
        )
      )
    composeRule.setContent {
      BottomSheetListView(
        bottomSheetHolder = bottomSheetHolder,
        onBottomSheetListener = onClickedListItems
      )
    }
  }
}
