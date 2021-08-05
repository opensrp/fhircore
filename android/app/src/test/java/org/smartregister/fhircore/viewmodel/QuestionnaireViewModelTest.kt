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

package org.smartregister.fhircore.viewmodel

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.QuestionnaireActivityTest
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.shadow.FhirApplicationShadow

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 03-07-2021. */
@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireViewModelTest : RobolectricTest() {

  private lateinit var questionnaireViewModel: QuestionnaireViewModel

  @Before
  fun setUp() {
    // MockKAnnotations.init(this, relaxUnitFun = true)
    val savedState = SavedStateHandle()
    savedState[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY] = "patient-registration.json"
    questionnaireViewModel =
      spyk(QuestionnaireViewModel(ApplicationProvider.getApplicationContext(), savedState))
  }

  @Test
  fun `saveBundleResources() should call saveResources()`() {
    val bundle = Bundle()
    val size = 23

    for (i in 1..size) {
      val bundleEntry = Bundle.BundleEntryComponent()
      bundleEntry.resource =
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                family = "Doe"
                given = listOf(StringType("John"))
              }
            )
        }
      bundle.addEntry(bundleEntry)
    }
    bundle.total = size

    every { questionnaireViewModel.saveResource(any()) } just runs

    // call the method under test
    questionnaireViewModel.saveBundleResources(bundle)

    verify(exactly = size) { questionnaireViewModel.saveResource(any()) }
  }

  @Test
  fun `saveBundleResources() should call saveResources and inject resourceId()`() {
    val bundle = Bundle()
    val size = 5
    val resource = slot<Resource>()
    val resourceId = "my-res-id"

    val bundleEntry = Bundle.BundleEntryComponent()
    bundleEntry.resource =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
      }
    bundle.addEntry(bundleEntry)
    bundle.total = size

    every { questionnaireViewModel.saveResource(any()) } just runs

    // call the method under test
    questionnaireViewModel.saveBundleResources(bundle, resourceId)

    verify(exactly = 1) { questionnaireViewModel.saveResource(capture(resource)) }

    Assert.assertEquals(resourceId, resource.captured.id)
  }

  @Test
  fun `fetchStructureMap() should call fhirEngine load and parse out the resourceId`() {
    val structureMap = StructureMap()
    val structureMapIdSlot = slot<String>()

    val fhirEngineMock = mockk<FhirEngine>()
    ReflectionHelpers.setField(
      FhirApplication.getContext(),
      "fhirEngine\$delegate",
      lazy { fhirEngineMock }
    )

    coEvery { fhirEngineMock.load(any<Class<StructureMap>>(), any()) } returns structureMap

    questionnaireViewModel.fetchStructureMap(
      ApplicationProvider.getApplicationContext(),
      "https://someorg.org/StructureMap/678934"
    )

    coVerify(exactly = 1) {
      fhirEngineMock.load(any<Class<StructureMap>>(), capture(structureMapIdSlot))
    }

    Assert.assertEquals("678934", structureMapIdSlot.captured)
  }

  @Test
  fun `saveExtractedResources() should call saveBundleResources and pass intent extra resourceId`() {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val questionnaire = Questionnaire()
    questionnaire.extension.add(
      Extension(
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext",
        Expression().apply {
          language = "application/x-fhir-query"
          expression = "Patient"
        }
      )
    )
    val questionnaireResponse = QuestionnaireResponse()
    val intent = Intent()
    val resourceId = "0993ldsfkaljlsnldm"
    intent.putExtra(PatientDetailFragment.ARG_ITEM_ID, resourceId)

    val resourceIdSlot = slot<String>()

    every { questionnaireViewModel.saveBundleResources(any(), any()) } just runs

    questionnaireViewModel.saveExtractedResources(
      ApplicationProvider.getApplicationContext(),
      intent,
      questionnaire,
      questionnaireResponse
    )

    coVerify(exactly = 1) {
      questionnaireViewModel.saveBundleResources(any(), capture(resourceIdSlot))
    }

    Assert.assertEquals(resourceId, resourceIdSlot.captured)
  }

  @Test
  fun testVerifyQuestionnaireSubjectType() {
    Assert.assertNotNull(questionnaireViewModel.questionnaire)
    Assert.assertEquals("Patient", questionnaireViewModel.questionnaire.subjectType[0])
  }

  @Test
  fun testVerifySavedResource() {
    val sourcePatient = QuestionnaireActivityTest.TEST_PATIENT_1

    questionnaireViewModel.saveResource(sourcePatient)
    val patient = runBlocking {
      FhirApplication.fhirEngine(FhirApplication.getContext())
        .load(Patient::class.java, QuestionnaireActivityTest.TEST_PATIENT_1_ID)
    }

    Assert.assertNotNull(patient)
    Assert.assertEquals(sourcePatient.logicalId, patient.logicalId)
  }

  @Test
  fun `getStructureMapProvider() should return valid provider`() {
    Assert.assertNull(questionnaireViewModel.structureMapProvider)

    Assert.assertNotNull(
      questionnaireViewModel.getStructureMapProvider(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun `structureMapProvider should call fetchStructureMap()`() {
    val resourceUrl = "https://fhir.org/StructureMap/89"
    val structureMapProvider =
      questionnaireViewModel.getStructureMapProvider(ApplicationProvider.getApplicationContext())

    every { questionnaireViewModel.fetchStructureMap(any(), any()) } returns StructureMap()

    structureMapProvider.invoke(resourceUrl)

    verify { questionnaireViewModel.fetchStructureMap(any(), resourceUrl) }
  }
}
