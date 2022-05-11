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

package org.smartregister.fhircore.engine.ui.register

import android.content.Context
import androidx.fragment.app.commitNow
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.hl7.fhir.r4.model.Patient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent

@HiltAndroidTest
class BaseRegisterFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  val registerFragment: BaseRegisterFragment<Patient, Patient> = spyk()

  lateinit var registerDataViewModel: RegisterDataViewModel<Patient, Patient>

  @Before
  fun setUp() {

    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }

    val allRegisterData: MutableStateFlow<Flow<PagingData<Patient>>> = MutableStateFlow(emptyFlow())

    registerDataViewModel =
      mockk {
        every { registerData } returns allRegisterData
        every { showResultsCount } returns MutableLiveData(false)
        every { showLoader } returns MutableLiveData(false)
        every { currentPage() } returns 1
        every { countPages() } returns 1
        every { filterRegisterData(any(), any(), any()) } returns Unit
        every { reloadCurrentPageData(refreshTotalRecordsCount = true) } just runs
      }
    registerFragment.registerDataViewModel = registerDataViewModel
    every { registerFragment.registerViewModel } returns mockk(relaxed = true)
  }

  @Test
  fun testOnViewCreatedConfiguresLiveDataObservationsCorrectly() {

    every { registerFragment.registerViewModel.lastSyncTimestamp } returns mockk(relaxed = true)
    every { registerFragment.registerViewModel.lastSyncTimestamp.observe(any(), any()) } just runs

    every { registerFragment.registerViewModel.refreshRegisterData } returns mockk(relaxed = true)
    every { registerFragment.registerViewModel.refreshRegisterData.observe(any(), any()) } just runs

    every { registerFragment.registerViewModel.filterValue } returns mockk(relaxed = true)
    every { registerFragment.registerViewModel.filterValue.observe(any(), any()) } just runs

    every { registerFragment.viewLifecycleOwner } returns mockk(relaxed = true)
    every { registerFragment.initializeRegisterDataViewModel() } returns mockk(relaxed = true)

    val activity =
      Robolectric.buildActivity(HiltActivityForTest::class.java).create().resume().get()
    activity.supportFragmentManager.commitNow {
      add(registerFragment, TestRegisterFragment::class.java.canonicalName)
    }

    registerFragment.onViewCreated(mockk(), mockk())

    verify(atLeast = 1) { registerFragment.initializeRegisterDataViewModel() }

    verify { registerFragment.registerViewModel.filterValue }
    verify { registerFragment.registerViewModel.filterValue.observe(any(), any()) }

    verify { registerFragment.registerViewModel.lastSyncTimestamp }
    verify { registerFragment.registerViewModel.lastSyncTimestamp.observe(any(), any()) }

    verify { registerFragment.registerViewModel.refreshRegisterData }
    verify { registerFragment.registerViewModel.refreshRegisterData.observe(any(), any()) }

    activity.finish()
  }

  @Test
  fun testOnResumeOnInvokationReloadsCurrentPageData() {
    every { registerDataViewModel.reloadCurrentPageData(refreshTotalRecordsCount = true) } just runs
    registerFragment.onResume()
    verify { registerDataViewModel.reloadCurrentPageData(refreshTotalRecordsCount = true) }
  }

  class TestRegisterFragment<patient : Patient, patientB : Patient> :
    BaseRegisterFragment<patient, patientB>() {
    override fun navigateToDetails(uniqueIdentifier: String) = Unit

    override fun onItemClicked(listenerIntent: ListenerIntent, data: patientB) = Unit

    override fun initializeRegisterDataViewModel(): RegisterDataViewModel<patient, patientB> {
      return registerDataViewModel
    }

    override fun performFilter(
      registerFilterType: RegisterFilterType,
      data: patientB,
      value: Any
    ): Boolean = true
  }
}
