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

package org.smartregister.fhircore.anc.ui.family

import androidx.fragment.app.commitNow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.shadow.FakeKeyStore
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.SecureSharedPreference

@Config(
  shadows =
    [AncApplicationShadow::class, FamilyRegisterFragmentTest.SecureSharedPreferenceShadow::class]
)
class FamilyRegisterFragmentTest : RobolectricTest() {

  private lateinit var registerFragment: FamilyRegisterFragment

  @Before
  fun setUp() {

    val registerActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().resume().get()
    registerFragment = FamilyRegisterFragment()
    registerActivity.supportFragmentManager.commitNow { add(registerFragment, "") }
  }

  @Test
  fun testPerformSearchFilterShouldReturnTrue() {
    val familyItem =
      FamilyItem("fid", "1111", "Name ", "M", "27", "Nairobi", true, emptyList(), 0, 0)

    val result =
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val familyItem =
      FamilyItem("fid", "1111", "Name ", "M", "27", "Nairobi", true, emptyList(), 4, 5)

    val result =
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }

  @Implements(SecureSharedPreference::class)
  class SecureSharedPreferenceShadow : Shadows() {

    @Implementation
    fun retrieveSessionUsername(): String {
      return "demo"
    }
  }
}
