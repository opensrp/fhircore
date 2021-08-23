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

package org.smartregister.fhircore.fragment

import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.FamilyListActivity
import org.smartregister.fhircore.adapter.FamilyItemRecyclerViewAdapter
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.model.FamilyMemberItem
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.viewmodel.FamilyListViewModel

@Config(shadows = [FhirApplicationShadow::class])
class FamilyListFragmentTest : FragmentRobolectricTest() {

  private lateinit var familyListFragment: FamilyListFragment
  private lateinit var familyListActivity: FamilyListActivity
  private lateinit var fragmentScenario: FragmentScenario<FamilyListFragment>
  private lateinit var listViewModel: FamilyListViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = spyk()
    // to suppress loadCount in BaseViewModel
    coEvery { fhirEngine.count(any()) } returns 2
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    familyListActivity = Robolectric.buildActivity(FamilyListActivity::class.java).create().get()
    listViewModel = spyk(familyListActivity.listViewModel)
    familyListActivity.listViewModel = listViewModel

    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(FamilyListFragment())
              every { fragment.activity } returns familyListActivity
              every { fragment.listViewModel } returns listViewModel
              return fragment
            }
          }
      )

    fragmentScenario.onFragment { familyListFragment = it }
  }

  @Test
  fun testEmptyListMessageWithZeroClients() {
    familyListFragment.setData(
      Pair(mutableListOf(), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    assertEquals(View.VISIBLE, container.visibility)
  }

  @Test
  fun testEmptyListMessageWithNonZeroClients() {
    var family =
      FamilyItem(
        "12",
        "John Doe",
        "male",
        "1985-05-21",
        "0700 000 000",
        "123",
        "narobi",
        listOf(
          FamilyMemberItem("123", "Sami", "female", "1987-01-01", "37287328", "1222", "pregnant")
        ),
      )

    familyListFragment.setData(
      Pair(mutableListOf(family), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    assertEquals(View.GONE, container.visibility)
  }

  @Test
  fun testFragmentViewModelIsFamilyViewModel() {
    assertTrue(familyListFragment.listViewModel is FamilyListViewModel)
  }

  @Test
  fun testFragmentLayoutIsFamilyLayout() {
    assertEquals(R.layout.family_fragment_list, familyListFragment.getFragmentListLayout())
  }

  @Test
  fun testFragmentAdapterIsFamilyAdapter() {
    assertTrue(familyListFragment.adapter is FamilyItemRecyclerViewAdapter)
  }

  @Test
  fun testFragmentHasPagination() {
    assertNotNull(familyListFragment.paginationView)
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return familyListFragment
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
