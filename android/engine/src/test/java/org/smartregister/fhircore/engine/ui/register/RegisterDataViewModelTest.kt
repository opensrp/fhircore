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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType

class RegisterDataViewModelTest : RobolectricTest() {

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @get:Rule(order = 2) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val application = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val registerRepository = mockk<RegisterRepository<String, String>>(relaxed = true)

  private lateinit var registerDataViewModel: RegisterDataViewModel<String, String>

  @Before
  fun setUp() {
    coEvery { registerRepository.countAll() } returns 100
    coEvery { registerRepository.loadData(query = any(), pageNumber = 0, loadAll = false) } returns
      intArrayOf(100).map { it.toString() }

    registerDataViewModel =
      spyk(
        objToCopy =
          RegisterDataViewModel(application = application, registerRepository = registerRepository)
      )
  }

  @Test
  fun testUpdateViewConfiguration() {
    registerDataViewModel.updateViewConfigurations(
      RegisterViewConfiguration("appId", "classification")
    )
    Assert.assertEquals("appId", registerDataViewModel.registerViewConfiguration.value?.appId)
    Assert.assertEquals(
      "classification",
      registerDataViewModel.registerViewConfiguration.value?.configType
    )
    registerDataViewModel.updateViewConfigurations(
      RegisterViewConfiguration("newAppId", "newClassification")
    )
    Assert.assertEquals("newAppId", registerDataViewModel.registerViewConfiguration.value?.appId)
    Assert.assertEquals(
      "newClassification",
      registerDataViewModel.registerViewConfiguration.value?.configType
    )
  }

  @Test
  fun testClickNextPageShouldIncrementCurrentPageValue() {
    registerDataViewModel.nextPage()
    registerDataViewModel.nextPage()
    Assert.assertNotNull(registerDataViewModel.currentPage.value)
    Assert.assertEquals(2, registerDataViewModel.currentPage.value!!)
  }

  @Test
  fun testClickPreviousPageShouldDecrementCurrentPageValue() {
    // Decrement when current page is greater than 0
    registerDataViewModel.currentPage.value = 2
    registerDataViewModel.previousPage()
    Assert.assertEquals(1, registerDataViewModel.currentPage.value!!)

    // Do nothing when current page is 0
    registerDataViewModel.currentPage.value = 0
    registerDataViewModel.previousPage()
    Assert.assertEquals(0, registerDataViewModel.currentPage.value!!)
  }

  @Test
  fun testCurrentPage() {
    registerDataViewModel.currentPage.value = 2
    Assert.assertEquals(3, registerDataViewModel.currentPage())
  }

  @Test
  fun testCountPageShouldDivideTotalRecordsByLimit() {
    // Total records is 100, limit 20
    Assert.assertEquals(5, registerDataViewModel.countPages())
  }

  @Test
  fun testShowResultsCount() {
    registerDataViewModel.showResultsCount(true)
    Assert.assertNotNull(registerDataViewModel.showResultsCount.value)
    Assert.assertTrue(registerDataViewModel.showResultsCount.value!!)
    registerDataViewModel.showResultsCount(false)
    Assert.assertNotNull(registerDataViewModel.showResultsCount.value)
    Assert.assertFalse(registerDataViewModel.showResultsCount.value!!)
  }

  @Test
  fun testShowLoader() {
    registerDataViewModel.setShowLoader(true)
    Assert.assertNotNull(registerDataViewModel.showLoader.value)
    Assert.assertTrue(registerDataViewModel.showLoader.value!!)
    registerDataViewModel.setShowLoader(false)
    Assert.assertNotNull(registerDataViewModel.showLoader.value)
    Assert.assertFalse(registerDataViewModel.showLoader.value!!)
  }

  @Test
  fun testReloadCurrentPageData() {
    coEvery { registerRepository.countAll() } returns 50
    registerDataViewModel.currentPage.value = 1
    coroutineTestRule.runBlockingTest {
      registerDataViewModel.reloadCurrentPageData(true)
      verify { registerDataViewModel.loadPageData(1) }

      // Total count is updated to 50, count pages = ceil(50/20)
      Assert.assertEquals(3, registerDataViewModel.countPages())
    }
  }

  @Test
  fun testFilterRegisterData() {
    coroutineTestRule.runBlockingTest {
      registerDataViewModel.filterRegisterData(RegisterFilterType.SEARCH_FILTER, "20") {
        _: RegisterFilterType,
        content: String,
        _: Any ->
        content.isNotEmpty()
      }
      Assert.assertNotNull(registerDataViewModel.registerData.value)
    }
  }
}
