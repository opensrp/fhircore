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

package org.smartregister.fhirecore.quest.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest

class RegisterViewConfigTest : RobolectricTest() {

  @Test
  fun testLoadRegisterViewConfigShouldReturnValidConfig() {
    val result =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterViewConfiguration("quest-patient-register")

    assertEquals("quest-patient-register", result.appId)
    assertEquals("Quest", result.appTitle)
    assertEquals("Show overdue", result.filterText)
    assertEquals("Search by ID or Name", result.searchBarHint)
    assertEquals("New Client", result.newClientButtonText)
    assertEquals(true, result.showSearchBar)
    assertEquals(true, result.showFilter)
    assertEquals(true, result.switchLanguages)
    assertEquals(true, result.showScanQRCode)
    assertEquals(true, result.showNewClientButton)
    assertEquals("patient-registration", result.registrationForm)
  }
}
