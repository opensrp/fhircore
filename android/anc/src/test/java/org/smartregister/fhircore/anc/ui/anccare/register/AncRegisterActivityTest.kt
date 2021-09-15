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

package org.smartregister.fhircore.anc.ui.anccare.register

import android.app.Activity
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.shadow.FakeKeyStore

class AncRegisterActivityTest : ActivityRobolectricTest() {

  private lateinit var ancRegisterActivity: AncRegisterActivity

  @Before
  fun setUp() {
    ancRegisterActivity =
      Robolectric.buildActivity(AncRegisterActivity::class.java).create().resume().get()
  }

  @Test
  fun testSideMenuOptionsShouldReturnAncMenuOptions() {
    val menu = ancRegisterActivity.sideMenuOptions()

    Assert.assertEquals(1, menu.size)
    with(menu.first()) {
      Assert.assertEquals(R.id.menu_item_anc, itemId)
      Assert.assertEquals(R.string.app_name, titleResource)
      Assert.assertTrue(opensMainRegister)
      Assert.assertEquals(
        org.robolectric.Shadows.shadowOf(
            androidx.core.content.ContextCompat.getDrawable(
              ancRegisterActivity,
              R.drawable.ic_baby_mother
            )
          )
          .createdFromResId,
        org.robolectric.Shadows.shadowOf(iconResource).createdFromResId
      )
    }
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldReturnTrue() {
    Assert.assertTrue(ancRegisterActivity.onSideMenuOptionSelected(RoboMenuItem()))
  }

  @Test
  fun testSupportedFragmentsShouldReturnPatientRegisterFragmentList() {
    val fragments = ancRegisterActivity.supportedFragments()

    Assert.assertEquals(1, fragments.size)
    Assert.assertEquals(
      AncRegisterFragment::class.java.simpleName,
      fragments.first().javaClass.simpleName
    )
  }

  override fun getActivity(): Activity {
    return ancRegisterActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
