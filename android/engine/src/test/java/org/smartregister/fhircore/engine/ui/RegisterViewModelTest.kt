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

package org.smartregister.fhircore.engine.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.register.RegisterViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.runOneTimeSync

class RegisterViewModelTest : RobolectricTest() {

  private lateinit var viewModel: RegisterViewModel

  @Before
  fun setUp() {

    val context = ApplicationProvider.getApplicationContext<Application>()
    SharedPreferencesHelper.init(context)

    val dispatcher =
      mockk<DispatcherProvider> {
        every { io() } returns DefaultDispatcherProvider.main()
        every { main() } returns DefaultDispatcherProvider.main()
      }

    viewModel = RegisterViewModel(context, mockk(), dispatcher)
  }

  @Test
  fun testUpdateViewConfigurationsShouldUpdateGlobalVariable() {
    viewModel.updateViewConfigurations(
      mockk {
        every { appId } returns "appId"
        every { appTitle } returns "Covax"
      }
    )

    assertEquals("appId", viewModel.registerViewConfiguration.value?.appId)
    assertEquals("Covax", viewModel.registerViewConfiguration.value?.appTitle)
  }

  @Test
  fun testLoadLanguagesShouldLoadEnglishLocaleOnly() {
    viewModel.loadLanguages()

    Assert.assertEquals(2, viewModel.languages.size)
    Assert.assertEquals("English", viewModel.languages[0].displayName)
  }

  @Test
  fun testRunSyncShouldRunOnlyOnce() {

    mockkStatic(Application::runOneTimeSync)

    val app = mockk<Application> { coEvery { runOneTimeSync(any()) } returns Unit }

    val viewModelSpy = spyk(viewModel) { every { getApplication<Application>() } returns app }

    viewModelSpy.runSync()

    shadowOf(Looper.getMainLooper()).idle()
    coVerify(exactly = 1) { app.runOneTimeSync(any()) }

    unmockkStatic(Application::runOneTimeSync)
  }

  @Test
  fun testUpdateFilterValueShouldUpdateGlobalFilter() {
    viewModel.updateFilterValue(RegisterFilterType.OVERDUE_FILTER, true)

    assertEquals(RegisterFilterType.OVERDUE_FILTER, viewModel.filterValue.value?.first)
    assertTrue(viewModel.filterValue.value?.second as Boolean)
  }

  @Test
  fun testSetRefreshRegisterDataShouldUpdateGlobalRegisterDate() {
    viewModel.setRefreshRegisterData(true)
    assertTrue(viewModel.refreshRegisterData.value!!)
  }

  @Test
  fun testSetLastSyncTimestampShouldUpdateGlobalSyncTimestamp() {
    viewModel.setLastSyncTimestamp("12345")
    assertEquals("12345", viewModel.lastSyncTimestamp.value)
    assertEquals("12345", SharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null))
  }
}
