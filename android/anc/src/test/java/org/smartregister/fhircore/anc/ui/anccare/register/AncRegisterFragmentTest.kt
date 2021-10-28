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

import android.content.Intent
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.shadow.FakeKeyStore
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType

@Config(shadows = [AncApplicationShadow::class])
class AncRegisterFragmentTest : RobolectricTest() {

  private lateinit var registerFragment: AncRegisterFragment

  @Before
  fun setUp() {

    val registerActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().resume().get()
    registerFragment = AncRegisterFragment()
    registerActivity.supportFragmentManager.commitNow { add(registerFragment, "") }
  }

  @Test
  fun testNavigateToDetailsShouldGotoToAncDetailsActivity() {

    val patientItem = AncPatientItem(patientIdentifier = "test_patient")
    registerFragment.onItemClicked(OpenPatientProfile, patientItem)

    val expectedIntent = Intent(registerFragment.context, AncDetailsActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<AncApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testPerformFilterShouldReturnTrue() {

    Assert.assertTrue(
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, AncPatientItem(), "")
    )
    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        AncPatientItem(patientIdentifier = "12345"),
        "12345"
      )
    )
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val result =
      registerFragment.performFilter(
        RegisterFilterType.OVERDUE_FILTER,
        AncPatientItem(patientIdentifier = "12345", visitStatus = VisitStatus.OVERDUE),
        "12345"
      )
    Assert.assertTrue(result)
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
