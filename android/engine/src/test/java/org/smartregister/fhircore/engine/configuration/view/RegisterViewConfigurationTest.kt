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
  }

  @Test
  fun testSearchFilter() {
    val searchFilter = SearchFilter("key", "code", "system")
    Assert.assertEquals("key", searchFilter.key)
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
          showBottomMenu = false
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
  }
}
