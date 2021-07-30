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

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.material.switchmaterial.SwitchMaterial
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.PatientDetailActivity
import org.smartregister.fhircore.activity.CovaxListActivity
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory

/**
 * The PatientListActivity should be removed from this test in favour FragmentScenario once the
 * fragment and activity are decoupled. The search and sync functionality is shared between the
 * fragment and activity causing the coupling
 */
@Config(shadows = [FhirApplicationShadow::class])
class PatientListFragmentTest : FragmentRobolectricTest() {

  private lateinit var patientListFragment: PatientListFragment
  private lateinit var patientListActivity: CovaxListActivity
  private lateinit var fragmentScenario: FragmentScenario<PatientListFragment>
  private lateinit var patientListViewModel: PatientListViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk()

    patientListActivity = Robolectric.buildActivity(CovaxListActivity::class.java).create().get()
    patientListViewModel =
      ViewModelProvider(
          patientListActivity,
          PatientListViewModelFactory(patientListActivity.application, fhirEngine)
        )
        .get(PatientListViewModel::class.java)

    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(PatientListFragment())
              every { fragment.activity } returns patientListActivity
              return fragment
            }
          }
      )

    fragmentScenario.onFragment { patientListFragment = it }
  }

  @Test
  fun testOnPatientItemClicked() {

    val id = "49333c33-f50f-4c3e-abd4-7aeb0f160ac2"
    val logicalId = "812983127"
    val patientItem =
      PatientItem(
        id,
        "John Doe",
        "male",
        "1985-05-21",
        "somehtml",
        "0700 000 000",
        logicalId,
        "high risk",
        lastSeen = "07-26-2021"
      )

    shadowOf(Looper.getMainLooper()).idle()

    patientListFragment.onPatientItemClicked(PatientListFragment.Intention.VIEW, patientItem)

    val shadowActivity = Shadows.shadowOf(patientListActivity)
    val startedActivityIntent = shadowActivity.peekNextStartedActivity()

    Assert.assertEquals(
      logicalId,
      startedActivityIntent.getStringExtra(PatientDetailFragment.ARG_ITEM_ID)
    )
    Assert.assertEquals(
      PatientDetailActivity::class.java.name,
      startedActivityIntent.component?.className
    )
  }

  @Test
  fun testEmptyListMessageWithZeroClients() {
    shadowOf(Looper.getMainLooper()).idle()

    patientListFragment.setData(
      Pair(mutableListOf(), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    Assert.assertEquals(View.VISIBLE, container.visibility)
  }

  @Test
  fun testEmptyListMessageWithNonZeroClients() {
    var patient =
      PatientItem(
        "12",
        "John Doe",
        "male",
        "1985-05-21",
        "somehtml",
        "0700 000 000",
        "test_id",
        "high risk",
        lastSeen = "07-26-2021"
      )
    shadowOf(Looper.getMainLooper()).idle()

    patientListFragment.patientListViewModel.liveSearchedPaginatedPatients.value =
      Pair(mutableListOf(patient), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))

    patientListFragment.setData(
      Pair(mutableListOf(patient), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    Assert.assertEquals(View.GONE, container.visibility)
  }

  @Test
  fun testVerifySyncResource() {

    val patientListViewModelSpy = spyk(patientListViewModel)
    patientListFragment.patientListViewModel = patientListViewModelSpy

    val btnSync = getView<View>(R.id.tv_sync)
    btnSync.performClick()

    every { patientListViewModelSpy.searchResults(pageSize = any()) } returns Unit
    every { patientListViewModelSpy.runSync() } returns Unit

    verify(exactly = 1) { patientListViewModelSpy.searchResults(pageSize = any()) }
    verify(exactly = 1) { patientListViewModelSpy.runSync() }

    patientListFragment.patientListViewModel = patientListViewModel
  }

  @Test
  fun testPatientIdFound() {
    val p = Patient()
    p.id = "test-id-existing"

    coEvery { fhirEngine.load(Patient::class.java, "test-id-existing") } returns p

    val result = patientListViewModel.isPatientExists("test-id-existing")

    result.observe(patientListFragment, { assertTrue(it.isSuccess) })
  }

  @Test
  fun testPatientIdNotFound() {
    val p = Patient()
    p.id = "test-id-missing"

    coEvery { fhirEngine.load(Patient::class.java, "test-id-missing") } throws
      ResourceNotFoundException("", "")

    val result = patientListViewModel.isPatientExists("test-id-missing")

    result.observe(patientListFragment, { assertTrue(it.isFailure) })
  }

  @Test
  fun testSearchResultsPaginatedPatientsShouldReturnEmptyData() {

    val patientListViewModelSpy = spyk(patientListViewModel)
    patientListFragment.patientListViewModel = patientListViewModelSpy

    patientListViewModelSpy.showOverduePatientsOnly.value = true
    getView<EditText>(R.id.edit_text_search).setText("unknown")

    every { patientListViewModelSpy.liveSearchedPaginatedPatients } answers
      {
        MutableLiveData<Pair<List<PatientItem>, Pagination>>(Pair(listOf(), Pagination(-1, 10, 0)))
      }

    val patients = patientListViewModelSpy.liveSearchedPaginatedPatients.value?.first
    val pagination = patientListViewModelSpy.liveSearchedPaginatedPatients.value?.second

    Assert.assertEquals(0, patients?.size)
    Assert.assertEquals(-1, pagination?.totalItems)
    Assert.assertEquals(0, pagination?.currentPage)
    Assert.assertEquals(10, pagination?.pageSize)

    patientListFragment.patientListViewModel = patientListViewModel
  }

  @Test
  fun testShowOverduePatientsOnlyShouldReturnTrue() {
    getView<SwitchMaterial>(R.id.btn_show_overdue_patients).isChecked = true
    assertTrue(patientListFragment.patientListViewModel.showOverduePatientsOnly.value!!)

    getView<SwitchMaterial>(R.id.btn_show_overdue_patients).isChecked = false
    assertFalse(patientListFragment.patientListViewModel.showOverduePatientsOnly.value!!)
  }

  @Test
  fun testLaunchBarcodeReaderShouldVerifyInternalCalls() {
    mockkStatic(ContextCompat::class)
    every { ContextCompat.checkSelfPermission(any(), any()) } returns
      PackageManager.PERMISSION_GRANTED

    val activityResultLauncher = mockk<ActivityResultLauncher<String>>()
    every { activityResultLauncher.launch(any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      patientListFragment,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )

    verify(exactly = 0) { activityResultLauncher.launch(any()) }

    every { ContextCompat.checkSelfPermission(any(), any()) } returns
      PackageManager.PERMISSION_DENIED
    ReflectionHelpers.callInstanceMethod<Any>(
      patientListFragment,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )

    verify(exactly = 1) { activityResultLauncher.launch(any()) }
  }

  @Test
  fun testOnNavigationClickedPageNo() {
    val patientListViewModelSpy = spyk(patientListViewModel)
    patientListFragment.patientListViewModel = patientListViewModelSpy

    every { patientListViewModelSpy.searchResults(any(), any(), any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      patientListFragment,
      "onNavigationClicked",
      ReflectionHelpers.ClassParameter(
        NavigationDirection::class.java,
        NavigationDirection.PREVIOUS
      ),
      ReflectionHelpers.ClassParameter(Int::class.java, 1)
    )

    verify(exactly = 1) { patientListViewModelSpy.searchResults(any(), eq(0), any()) }

    ReflectionHelpers.callInstanceMethod<Any>(
      patientListFragment,
      "onNavigationClicked",
      ReflectionHelpers.ClassParameter(NavigationDirection::class.java, NavigationDirection.NEXT),
      ReflectionHelpers.ClassParameter(Int::class.java, 1)
    )

    verify(exactly = 1) { patientListViewModelSpy.searchResults(any(), eq(2), any()) }

    patientListFragment.patientListViewModel = patientListViewModel
  }

  @Test
  fun testLaunchPatientDetailActivityShouldStartPatientDetailActivity() {
    ReflectionHelpers.callInstanceMethod<Any>(
      patientListFragment,
      "launchPatientDetailActivity",
      ReflectionHelpers.ClassParameter(String::class.java, "0")
    )

    val expectedIntent = Intent(patientListActivity, PatientDetailActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<FhirApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  private fun <T : View?> getView(id: Int): T {
    return patientListFragment.requireActivity().findViewById<T>(id)
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
