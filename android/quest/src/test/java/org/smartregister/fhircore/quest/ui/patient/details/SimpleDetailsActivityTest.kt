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

package org.smartregister.fhircore.quest.ui.patient.details

import android.content.Intent
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import javax.inject.Inject
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.details.SimpleDetailsActivity.Companion.RECORD_ID_ARG
import timber.log.Timber

@HiltAndroidTest
class SimpleDetailsActivityTest : RobolectricTest() {

  private lateinit var simpleDetailsActivity: SimpleDetailsActivity

  @BindValue val patientRepository: PatientRepository = mockk()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @Before
  fun setUp() {
    Faker.initPatientRepositoryMocks(patientRepository)

    val intent = Intent().putExtra(RECORD_ID_ARG, "1234")
    simpleDetailsActivity =
      Robolectric.buildActivity(SimpleDetailsActivity::class.java, intent).create().resume().get()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    simpleDetailsActivity.finish()
  }

  @Test
  fun testOnBackPressListenerShouldCallFinishActivity() {
    simpleDetailsActivity.viewModel.onBackPressed(true)
    Assert.assertTrue(simpleDetailsActivity.isFinishing)
  }

  @Test
  fun testG6pdPatientRegistrationExtraction() {
    val g6pdStructureMap = "patient-registration-questionnaire/structure-map.txt".readFile()
    val g6pdResponse = "patient-registration-questionnaire/questionnaire-response.json".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(g6pdStructureMap, "PatientRegistration")

    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    Timber.d(mapString)

    val targetResource = Bundle()

    val baseElement = iParser.parseResource(QuestionnaireResponse::class.java, g6pdResponse)

    kotlin.runCatching { scu.transform(contextR4, baseElement, map, targetResource) }.onFailure {
      Timber.d(it.stackTraceToString())
    }

    println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun testG6pdTestResultsExtraction() {
    val g6pdStructureMap = "test-results-questionnaire/structure-map.txt".readFile()
    val g6pdResponse = "test-results-questionnaire/questionnaire-response.json".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(g6pdStructureMap, "TestResults")

    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    Timber.d(mapString)

    val targetResource = Bundle()

    val baseElement = iParser.parseResource(QuestionnaireResponse::class.java, g6pdResponse)

    kotlin.runCatching { scu.transform(contextR4, baseElement, map, targetResource) }.onFailure {
      Timber.d(it.stackTraceToString())
    }

    Timber.d(iParser.encodeResourceToString(targetResource))
  }
}
