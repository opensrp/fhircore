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
        newClientButtonText = "newClientButtonText",
        registrationForm = "patient-registration",
      )
    Assert.assertEquals("anc", registerViewConfiguration.appId)
    Assert.assertEquals("appTitle", registerViewConfiguration.appTitle)
    Assert.assertEquals("newClientButtonText", registerViewConfiguration.newClientButtonText)
  }

  @Test
  fun testDefaultRegisterViewConfiguration() {
    val registerViewConfiguration = RegisterViewConfiguration("anc", "classification")
    Assert.assertEquals("anc", registerViewConfiguration.appId)
    Assert.assertEquals("", registerViewConfiguration.appTitle)
    Assert.assertEquals("", registerViewConfiguration.newClientButtonText)
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
          newClientButtonText = "newClientButtonText",
          registrationForm = "patient-reg-form",
        )
    Assert.assertEquals("anc", registerViewConfigurationOf.appId)
    Assert.assertEquals("clasification", registerViewConfigurationOf.classification)
    Assert.assertEquals("appTitle", registerViewConfigurationOf.appTitle)
    Assert.assertEquals("newClientButtonText", registerViewConfigurationOf.newClientButtonText)
    Assert.assertEquals("patient-reg-form", registerViewConfigurationOf.registrationForm)
  }

  @Test
  fun testDefaultRegisterViewConfigurationOf() {
    val registerViewConfigurationOf =
      ApplicationProvider.getApplicationContext<Application>().registerViewConfigurationOf()
    Assert.assertEquals("", registerViewConfigurationOf.appId)
    Assert.assertEquals("", registerViewConfigurationOf.classification)
    Assert.assertEquals("Fhir App", registerViewConfigurationOf.appTitle)
    Assert.assertEquals("Register new client", registerViewConfigurationOf.newClientButtonText)
    Assert.assertEquals("patient-registration", registerViewConfigurationOf.registrationForm)
  }
}
