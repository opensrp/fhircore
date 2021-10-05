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
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class RegisterViewConfigurationTest : RobolectricTest() {

  @Test
  fun testLoadRegisterViewConfigShouldReturnValidConfig() {
    val result =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterViewConfiguration("quest-app-patient-register")

    assertEquals("quest-app-patient-register", result.id)
    assertEquals("Clients", result.appTitle)
    assertEquals("Show overdue", result.filterText)
    assertEquals("Search for ID or client name", result.searchBarHint)
    assertEquals("Add new client", result.newClientButtonText)
    assertEquals("rounded_corner", result.newClientButtonStyle)
    assertEquals(true, result.showSearchBar)
    assertEquals(false, result.showFilter)
    assertEquals(true, result.switchLanguages)
    assertEquals(false, result.showScanQRCode)
    assertEquals(true, result.showNewClientButton)
    assertEquals(false, result.showSideMenu)
    assertEquals(true, result.showBottomMenu)
    assertEquals("patient-registration", result.registrationForm)
  }
}
