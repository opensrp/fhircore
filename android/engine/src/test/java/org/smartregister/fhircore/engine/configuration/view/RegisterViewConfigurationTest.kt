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
        "anc",
        "clasification",
        "appTitle",
        "filterText",
        "searchBarHint",
        "newClientButtonText",
        "newClientButtonStyle",
        false,
        true,
        true,
        true,
        true,
        "patient-registration",
        true,
        false,
        null
      )
    Assert.assertEquals("anc", registerViewConfiguration.appId)
    Assert.assertEquals("appTitle", registerViewConfiguration.appTitle)
    Assert.assertEquals("searchBarHint", registerViewConfiguration.searchBarHint)
    Assert.assertEquals("newClientButtonText", registerViewConfiguration.newClientButtonText)
    Assert.assertEquals("newClientButtonStyle", registerViewConfiguration.newClientButtonStyle)
    Assert.assertFalse(registerViewConfiguration.showSearchBar)
    Assert.assertTrue(registerViewConfiguration.switchLanguages)
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
          "anc",
          "clasification",
          "appTitle",
          "filterText",
          "searchBarHint",
          "newClientButtonText",
          false,
          true,
          true,
          true,
          true,
          "patient-registration",
          true,
          false,
          null
        )
    Assert.assertEquals("anc", registerViewConfigurationOf.appId)
    Assert.assertEquals("clasification", registerViewConfigurationOf.appTitle)
    Assert.assertEquals("filterText", registerViewConfigurationOf.searchBarHint)
    Assert.assertEquals("searchBarHint", registerViewConfigurationOf.newClientButtonText)
    Assert.assertEquals("newClientButtonText", registerViewConfigurationOf.newClientButtonStyle)
    Assert.assertFalse(registerViewConfigurationOf.showSearchBar)
    Assert.assertTrue(registerViewConfigurationOf.switchLanguages)
    Assert.assertTrue(registerViewConfigurationOf.showScanQRCode)
    Assert.assertTrue(registerViewConfigurationOf.showNewClientButton)
    Assert.assertTrue(registerViewConfigurationOf.showSideMenu)
    Assert.assertFalse(registerViewConfigurationOf.showBottomMenu)
  }
}
