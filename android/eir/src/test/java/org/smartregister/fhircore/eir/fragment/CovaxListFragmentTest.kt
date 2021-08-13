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

package org.smartregister.fhircore.eir.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.material.switchmaterial.SwitchMaterial
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.auth.secure.FakeKeyStore
import org.smartregister.fhircore.eir.domain.Pagination
import org.smartregister.fhircore.eir.model.PatientItem
import org.smartregister.fhircore.eir.shadow.FhirApplicationShadow
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsFormConfig
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsFragment
import org.smartregister.fhircore.eir.ui.patient.register.CovaxListActivity
import org.smartregister.fhircore.eir.ui.patient.register.NavigationDirection
import org.smartregister.fhircore.eir.viewmodel.CovaxListViewModel

/**
 * The covaxListActivity should be removed from this test in favour FragmentScenario once the
 * fragment and activity are decoupled. The search and sync functionality is shared between the
 * fragment and activity causing the coupling
 */
@Config(shadows = [FhirApplicationShadow::class])
class CovaxListFragmentTest : FragmentRobolectricTest() {

  private lateinit var covaxListFragment: CovaxListFragment
  private lateinit var covaxListActivity: CovaxListActivity
  private lateinit var fragmentScenario: FragmentScenario<CovaxListFragment>
  private lateinit var patientListViewModel: CovaxListViewModel
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = spyk()
    // to suppress loadCount in BaseViewModel
    coEvery { fhirEngine.count(any()) } returns 2
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    mockkObject(EirApplication)
    every { EirApplication.fhirEngine(any()) } returns fhirEngine

    covaxListActivity = Robolectric.buildActivity(CovaxListActivity::class.java).create().get()
    patientListViewModel = spyk(covaxListActivity.listViewModel)
    covaxListActivity.listViewModel = patientListViewModel

    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(CovaxListFragment())
              every { fragment.activity } returns covaxListActivity
              every { fragment.patientListViewModel } returns patientListViewModel
              return fragment
            }
          }
      )

    fragmentScenario.onFragment { covaxListFragment = it }
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

    covaxListFragment.onPatientItemClicked(CovaxListFragment.Intention.VIEW, patientItem)

    val shadowActivity = shadowOf(covaxListActivity)
    val startedActivityIntent = shadowActivity.peekNextStartedActivity()

    Assert.assertEquals(
      logicalId,
      startedActivityIntent.getStringExtra(PatientDetailsFormConfig.COVAX_ARG_ITEM_ID)
    )
    Assert.assertEquals(
      PatientDetailsActivity::class.java.name,
      startedActivityIntent.component?.className
    )
  }

  @Test
  fun testEmptyListMessageWithZeroClients() {
    shadowOf(Looper.getMainLooper()).idle()

    covaxListFragment.setData(
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

    covaxListFragment.patientListViewModel.liveSearchedPaginatedPatients.value =
      Pair(mutableListOf(patient), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))

    covaxListFragment.setData(
      Pair(mutableListOf(patient), Pagination(totalItems = 1, pageSize = 5, currentPage = 1))
    )
    val container = getView<LinearLayout>(R.id.empty_list_message_container)
    Assert.assertEquals(View.GONE, container.visibility)
  }

  @Test
  fun testVerifySyncResource() {
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    val btnSync = getView<View>(R.id.tv_sync)
    btnSync.performClick()

    every { patientListViewModel.searchResults(pageSize = any()) } returns Unit
    every { patientListViewModel.runSync(false) } returns Unit

    // one is from oncreate
    verify(exactly = 1) { patientListViewModel.runSync(false) }
  }

  @Test
  fun testLoadDataCallSyncOnZeroClient() {
    coEvery { patientListViewModel.count(any()) } returns 0
    every { patientListViewModel.searchResults(any(), any(), any()) } returns Unit
    every { patientListViewModel.runSync(any()) } returns Unit

    covaxListFragment.loadData()

    verify(exactly = 1) { patientListViewModel.runSync(true) }
    verify(exactly = 1) { patientListViewModel.searchResults(any(), any(), any()) }
  }

  @Test
  fun testLoadDataCallSearchResultsOnNonZeroClient() {
    coEvery { patientListViewModel.count(any()) } returns 3

    every { patientListViewModel.searchResults(any(), any(), any()) } returns Unit
    every { patientListViewModel.runSync(any()) } returns Unit

    covaxListFragment.loadData()

    verify(exactly = 2) { patientListViewModel.searchResults(any(), any(), any()) }
    verify(inverse = true) { patientListViewModel.runSync(any()) }
  }

  @Test
  fun testPatientIdFound() {
    val p = Patient()
    p.id = "test-id-existing"

    coEvery { fhirEngine.load(Patient::class.java, "test-id-existing") } returns p

    val result = patientListViewModel.isPatientExists("test-id-existing")

    result.observe(covaxListFragment, { assertTrue(it.isSuccess) })
  }

  @Test
  fun testPatientIdNotFound() {
    val p = Patient()
    p.id = "test-id-missing"

    coEvery { fhirEngine.load(Patient::class.java, "test-id-missing") } throws
      ResourceNotFoundException("", "")

    val result = patientListViewModel.isPatientExists("test-id-missing")

    result.observe(covaxListFragment, { assertTrue(it.isFailure) })
  }

  @Test
  fun testSearchResultsPaginatedPatientsShouldReturnEmptyData() {

    val patientListViewModelSpy = spyk(patientListViewModel)

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
  }

  @Test
  fun testShowOverduePatientsOnlyShouldReturnTrue() {
    getView<SwitchMaterial>(R.id.btn_show_overdue).isChecked = true
    assertTrue(covaxListFragment.patientListViewModel.showOverduePatientsOnly.value!!)

    getView<SwitchMaterial>(R.id.btn_show_overdue).isChecked = false
    assertFalse(covaxListFragment.patientListViewModel.showOverduePatientsOnly.value!!)
  }

  @Ignore("Barcode test need to be validated again")
  @Test
  fun testLaunchBarcodeReaderShouldVerifyInternalCalls() {
    mockkStatic(ContextCompat::class)
    every { ContextCompat.checkSelfPermission(any(), any()) } returns
      PackageManager.PERMISSION_GRANTED

    val activityResultLauncher = mockk<ActivityResultLauncher<String>>()
    every { activityResultLauncher.launch(any()) } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(
      covaxListFragment,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )

    verify(exactly = 0) { activityResultLauncher.launch(any()) }

    every { ContextCompat.checkSelfPermission(any(), any()) } returns
      PackageManager.PERMISSION_DENIED
    ReflectionHelpers.callInstanceMethod<Any>(
      covaxListFragment,
      "launchBarcodeReader",
      ReflectionHelpers.ClassParameter(ActivityResultLauncher::class.java, activityResultLauncher)
    )

    verify(exactly = 1) { activityResultLauncher.launch(any()) }
  }

  @Test
  fun testOnNavigationClickedPageNo() {
    coEvery { fhirEngine.search<Patient>(any()) } returns listOf()

    ReflectionHelpers.callInstanceMethod<Any>(
      covaxListFragment,
      "onNavigationClicked",
      ReflectionHelpers.ClassParameter(
        NavigationDirection::class.java,
        NavigationDirection.PREVIOUS
      ),
      ReflectionHelpers.ClassParameter(Int::class.java, 1)
    )

    verify(/*todo exactly = 1*/ ) { patientListViewModel.searchResults(any(), eq(0), any()) }

    /*    ReflectionHelpers.callInstanceMethod<Any>(
      covaxListFragment,
      "onNavigationClicked",
      ReflectionHelpers.ClassParameter(NavigationDirection::class.java, NavigationDirection.NEXT),
      ReflectionHelpers.ClassParameter(Int::class.java, 1)
    )

    verify(exactly = 1) { patientListViewModel.searchResults(any(), eq(2), any()) }*/
  }

  @Test
  fun testLaunchCovaxDetailActivityShouldStartCovaxDetailActivity() {
    val patientItem =
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
    covaxListFragment.onPatientItemClicked(CovaxListFragment.Intention.VIEW, patientItem)

    val expectedIntent = Intent(covaxListActivity, PatientDetailsActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<EirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Ignore("Stuck infinitely")
  @Test
  fun testSetUpBarcodeScannerVerifyCallbackBehaviour() {

    val patientListViewModelSpy = spyk(patientListViewModel)

    val fragmentManager = mockk<FragmentManager>()
    val requestKeySlot = slot<String>()
    val lifecycleOwnerSlot = slot<LifecycleOwner>()
    val fragmentResultListenerSlot = slot<FragmentResultListener>()

    val bundle = Bundle()
    bundle.putString("result", "data")

    every { patientListViewModelSpy.isPatientExists(any()) } returns
      MutableLiveData(Result.success(true))
    every {
      fragmentManager.setFragmentResultListener(
        capture(requestKeySlot),
        capture(lifecycleOwnerSlot),
        capture(fragmentResultListenerSlot)
      )
    } answers { fragmentResultListenerSlot.captured.onFragmentResult("", bundle) }
    every { covaxListActivity.supportFragmentManager } returns fragmentManager
    every {
      covaxListFragment.registerForActivityResult(
        any<ActivityResultContracts.RequestPermission>(),
        any()
      )
    } returns mockk()

    ReflectionHelpers.callInstanceMethod<Any>(covaxListFragment, "setUpBarcodeScanner")

    val expectedIntent =
      Intent(covaxListFragment.requireContext(), PatientDetailsFragment::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<EirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)

    every { patientListViewModelSpy.isPatientExists(any()) } returns
      MutableLiveData(Result.failure(mockk()))
    ReflectionHelpers.callInstanceMethod<Any>(covaxListFragment, "setUpBarcodeScanner")

    verify(exactly = 1) { covaxListActivity.startRegistrationActivity(any()) }
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return covaxListFragment
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
