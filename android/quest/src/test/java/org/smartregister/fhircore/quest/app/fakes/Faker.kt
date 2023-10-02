/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.app.fakes

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Basic
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.auth.AuthCredentials
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.ui.login.LoginActivity

object Faker {

  val authCredentials =
    AuthCredentials(username = "demo", salt = "ChUmvi", passwordHash = "GENERATED_PASSWORD_HASH")

  val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    useAlternativeNames = true
  }

  private const val APP_DEBUG = "app/debug"

  fun buildTestConfigurationRegistry(): ConfigurationRegistry {
    val fhirResourceService = mockk<FhirResourceService>()
    val fhirResourceDataSource = spyk(FhirResourceDataSource(fhirResourceService))
    coEvery { fhirResourceService.getResource(any()) } returns Bundle()

    val configurationRegistry =
      spyk(
        ConfigurationRegistry(
          fhirEngine = mockk(),
          fhirResourceDataSource = fhirResourceDataSource,
          sharedPreferencesHelper = mockk(),
          dispatcherProvider = mockk(),
          configService = mockk(),
          json = json,
        ),
      )

    coEvery { configurationRegistry.addOrUpdate(any()) } just runs

    runBlocking {
      configurationRegistry.loadConfigurations(
        appId = APP_DEBUG,
        context = ApplicationProvider.getApplicationContext(),
      ) {}
    }

    return configurationRegistry
  }

  fun buildSharedPreferencesHelper() =
    SharedPreferencesHelper(
      ApplicationProvider.getApplicationContext<Application>(),
      Gson(),
    )

  fun buildPatient(
    id: String = "sampleId",
    family: String = "Mandela",
    given: String = "Nelson",
    age: Int = 78,
    gender: Enumerations.AdministrativeGender = Enumerations.AdministrativeGender.MALE,
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }

  fun buildBasicResource(
    id: String = "sampleId",
  ): Basic {
    return Basic().apply {
      this.id = id
      this.identifierFirstRep.value = id
    }
  }

  open class TestLoginActivity : LoginActivity() {
    override fun pinActive() = true

    override fun pinEnabled() = true

    override fun deviceOnline() = false

    override fun isRefreshTokenActive() = true
  }

  open class TestLoginActivityInActivePin : LoginActivity() {
    override fun pinActive() = false

    override fun pinEnabled() = true

    override fun deviceOnline() = true
  }
}
