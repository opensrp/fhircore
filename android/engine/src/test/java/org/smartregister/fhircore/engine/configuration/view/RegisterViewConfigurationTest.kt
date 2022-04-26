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

package org.smartregister.fhircore.engine.configuration.view

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class RegisterViewConfigurationTest : RobolectricTest() {

  @Test
  fun testRegisterViewConfiguration() {
    val registerViewConfiguration =
      RegisterViewConfiguration(
        appId = "anc",
        classification = "clasification",
        appTitle = "appTitle",
        filterText = "filterText",
        searchBarHint = "searchBarHint",
        newClientButtonText = "newClientButtonText",
        newClientButtonStyle = "newClientButtonStyle",
        showSearchBar = false,
        showFilter = true,
        showScanQRCode = true,
        showNewClientButton = true,
        registrationForm = "patient-registration",
        showSideMenu = true,
        showBottomMenu = false,
        useLabel = true,
        showHeader = false,
        showFooter = false,
        primaryFilter = null
      )
    Assert.assertEquals("anc", registerViewConfiguration.appId)
    Assert.assertEquals("appTitle", registerViewConfiguration.appTitle)
    Assert.assertEquals("searchBarHint", registerViewConfiguration.searchBarHint)
    Assert.assertEquals("newClientButtonText", registerViewConfiguration.newClientButtonText)
    Assert.assertEquals("newClientButtonStyle", registerViewConfiguration.newClientButtonStyle)
    Assert.assertFalse(registerViewConfiguration.showSearchBar)
    Assert.assertTrue(registerViewConfiguration.showScanQRCode)
    Assert.assertTrue(registerViewConfiguration.showNewClientButton)
    Assert.assertTrue(registerViewConfiguration.showSideMenu)
    Assert.assertFalse(registerViewConfiguration.showBottomMenu)
    Assert.assertTrue(registerViewConfiguration.useLabel)
    Assert.assertFalse(registerViewConfiguration.showHeader)
    Assert.assertFalse(registerViewConfiguration.showFooter)
  }

  @Test
  fun testDefaultRegisterViewConfiguration() {
    val registerViewConfiguration = RegisterViewConfiguration("anc", "classification")
    Assert.assertEquals("anc", registerViewConfiguration.appId)
    Assert.assertEquals("", registerViewConfiguration.appTitle)
    Assert.assertEquals("", registerViewConfiguration.searchBarHint)
    Assert.assertEquals("", registerViewConfiguration.newClientButtonText)
    Assert.assertEquals("", registerViewConfiguration.newClientButtonStyle)
    Assert.assertTrue(registerViewConfiguration.showSearchBar)
    Assert.assertTrue(registerViewConfiguration.showScanQRCode)
    Assert.assertTrue(registerViewConfiguration.showNewClientButton)
    Assert.assertTrue(registerViewConfiguration.showSideMenu)
    Assert.assertFalse(registerViewConfiguration.showBottomMenu)
    Assert.assertTrue(registerViewConfiguration.useLabel)
    Assert.assertTrue(registerViewConfiguration.showHeader)
    Assert.assertTrue(registerViewConfiguration.showFooter)
  }

  @Test
  fun testSearchFilter() {
    // val searchFilter = SearchFilter("anc", "key", "code", "system")
    // Assert.assertEquals("key", searchFilter.key)
  }

  @Test
  fun testRegisterViewConfigurationOf() {
    val registerViewConfigurationOf =
      ApplicationProvider.getApplicationContext<Application>()
        .registerViewConfigurationOf(
          appId = "anc",
          classification = "clasification",
          appTitle = "appTitle",
          filterText = "filterText",
          searchBarHint = "searchBarHint",
          newClientButtonText = "newClientButtonText",
          newClientButtonStyle = "newClientButtonStyle",
          showSearchBar = true,
          showFilter = true,
          showScanQRCode = true,
          showNewClientButton = true,
          registrationForm = "patient-reg-form",
          showSideMenu = false,
          showBottomMenu = false,
          useLabel = true,
          showHeader = false,
          showFooter = false
        )
    Assert.assertEquals("anc", registerViewConfigurationOf.appId)
    Assert.assertEquals("clasification", registerViewConfigurationOf.classification)
    Assert.assertEquals("appTitle", registerViewConfigurationOf.appTitle)
    Assert.assertEquals("filterText", registerViewConfigurationOf.filterText)
    Assert.assertEquals("searchBarHint", registerViewConfigurationOf.searchBarHint)
    Assert.assertEquals("newClientButtonText", registerViewConfigurationOf.newClientButtonText)
    Assert.assertEquals("newClientButtonStyle", registerViewConfigurationOf.newClientButtonStyle)
    Assert.assertEquals("patient-reg-form", registerViewConfigurationOf.registrationForm)
    Assert.assertTrue(registerViewConfigurationOf.showSearchBar)
    Assert.assertTrue(registerViewConfigurationOf.showScanQRCode)
    Assert.assertTrue(registerViewConfigurationOf.showNewClientButton)
    Assert.assertFalse(registerViewConfigurationOf.showSideMenu)
    Assert.assertFalse(registerViewConfigurationOf.showBottomMenu)
    Assert.assertTrue(registerViewConfigurationOf.useLabel)
    Assert.assertFalse(registerViewConfigurationOf.showHeader)
    Assert.assertFalse(registerViewConfigurationOf.showFooter)
  }

  @Test
  fun testDefaultRegisterViewConfigurationOf() {
    val registerViewConfigurationOf =
      ApplicationProvider.getApplicationContext<Application>().registerViewConfigurationOf()
    Assert.assertEquals("", registerViewConfigurationOf.appId)
    Assert.assertEquals("", registerViewConfigurationOf.classification)
    Assert.assertEquals("Fhir App", registerViewConfigurationOf.appTitle)
    Assert.assertEquals("Show overdue", registerViewConfigurationOf.filterText)
    Assert.assertEquals("Search name or ID", registerViewConfigurationOf.searchBarHint)
    Assert.assertEquals("Register new client", registerViewConfigurationOf.newClientButtonText)
    Assert.assertEquals("", registerViewConfigurationOf.newClientButtonStyle)
    Assert.assertEquals("patient-registration", registerViewConfigurationOf.registrationForm)
    Assert.assertTrue(registerViewConfigurationOf.showSearchBar)
    Assert.assertTrue(registerViewConfigurationOf.showScanQRCode)
    Assert.assertTrue(registerViewConfigurationOf.showNewClientButton)
    Assert.assertTrue(registerViewConfigurationOf.showSideMenu)
    Assert.assertFalse(registerViewConfigurationOf.showBottomMenu)
    Assert.assertTrue(registerViewConfigurationOf.useLabel)
    Assert.assertTrue(registerViewConfigurationOf.showHeader)
    Assert.assertTrue(registerViewConfigurationOf.showFooter)
  }
}
