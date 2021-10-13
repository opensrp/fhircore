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
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Binary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.loadBinaryResourceConfiguration
import org.smartregister.fhircore.quest.QuestApplication
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

  @Test
  fun testLoadBinaryRegisterViewConfigurationShouldReturnValidConfig() = runBlockingTest {
    val expectedConfig =
      ApplicationProvider.getApplicationContext<Application>()
        .registerViewConfigurationOf(
          id = "test-config-id",
          appTitle = "My title",
          filterText = "Filter label",
          searchBarHint = "Search hint",
          newClientButtonText = "Add new client",
          newClientButtonStyle = "rounded_corner",
          showSearchBar = true,
          showFilter = true,
          switchLanguages = true,
          showScanQRCode = true,
          showNewClientButton = true,
          languages = listOf("en"),
          registrationForm = "patient-registration",
          showSideMenu = true,
          showBottomMenu = false
        )

    val context = ApplicationProvider.getApplicationContext<QuestApplication>()
    context.fhirEngine.save(
      Binary().apply {
        id = "test-config-id"
        data = expectedConfig.encodeJson().encodeToByteArray()
      }
    )

    val result =
      context.loadBinaryResourceConfiguration<RegisterViewConfiguration>("test-config-id")!!

    assertEquals("test-config-id", result.id)
    assertEquals("My title", result.appTitle)
    assertEquals("Filter label", result.filterText)
    assertEquals("Search hint", result.searchBarHint)
    assertEquals("Add new client", result.newClientButtonText)
    assertEquals("rounded_corner", result.newClientButtonStyle)
    assertEquals(true, result.showSearchBar)
    assertEquals(true, result.showFilter)
    assertEquals(true, result.switchLanguages)
    assertEquals(true, result.showScanQRCode)
    assertEquals(true, result.showNewClientButton)
    assertEquals(true, result.showSideMenu)
    assertEquals(false, result.showBottomMenu)
    assertEquals("patient-registration", result.registrationForm)
  }
}
