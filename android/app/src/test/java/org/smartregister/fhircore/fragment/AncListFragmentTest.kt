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
import io.mockk.verify
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
import org.smartregister.fhircore.activity.AncListActivity
import org.smartregister.fhircore.adapter.AncItemRecyclerViewAdapter
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.AncItem
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.viewmodel.AncListViewModel

@Config(shadows = [FhirApplicationShadow::class])
class AncListFragmentTest : FragmentRobolectricTest() {

  private lateinit var ancListFragment: AncListFragment
  private lateinit var ancListActivity: AncListActivity
  private lateinit var fragmentScenario: FragmentScenario<AncListFragment>
  private lateinit var listViewModel: AncListViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = spyk()
    // to suppress loadCount in BaseViewModel
    coEvery { fhirEngine.count(any()) } returns 2
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    ancListActivity = Robolectric.buildActivity(AncListActivity::class.java).create().get()
    listViewModel = spyk(ancListActivity.listViewModel)
    ancListActivity.listViewModel = listViewModel

    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(AncListFragment())
              every { fragment.activity } returns ancListActivity
              every { fragment.viewModel } returns listViewModel
              return fragment
            }
          }
      )

    fragmentScenario.onFragment { ancListFragment = it }
  }

  @Test
  fun testEmptyListMessageWithZeroClients() {
    ancListFragment.setData(
      Pair(mutableListOf(), Pagination(totalItems = 0, pageSize = 2, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    assertEquals(View.VISIBLE, container.visibility)
  }

  @Test
  fun testEmptyListMessageWithNonZeroClients() {
    var anc =
      AncItem(
        "12",
        "John Doe",
        "1985-05-21",
        "1122",
        "nairobi",
      )

    ancListFragment.setData(
      Pair(
        mutableListOf(anc, anc, anc, anc),
        Pagination(totalItems = 4, pageSize = 2, currentPage = 1)
      )
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    assertEquals(View.GONE, container.visibility)
  }

  @Test
  fun testFragmentViewModelIsAncViewModel() {
    assertTrue(ancListFragment.viewModel is AncListViewModel)
  }

  @Test
  fun testFragmentLayoutIsAncLayout() {
    assertEquals(R.layout.anc_fragment_list, ancListFragment.getFragmentListLayout())
  }

  @Test
  fun testFragmentAdapterIsAncAdapter() {
    assertTrue(ancListFragment.adapter is AncItemRecyclerViewAdapter)
  }

  @Test
  fun testFragmentHasPagination() {
    assertNotNull(ancListFragment.paginationView)
  }

  @Test
  fun testOnNavigationClickedNextPageNo() {
    ancListFragment.paginationView!!.nextButton!!.performClick()

    verify { listViewModel.searchResults(any(), any(), any()) }
  }

  @Test
  fun testOnNavigationClickedPrevPageNo() {
    ancListFragment.paginationView!!.prevButton!!.performClick()

    verify { listViewModel.searchResults(any(), any(), any()) }
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return ancListFragment
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
