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

package org.smartregister.fhircore.anc.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.utilities.SimpleWorkerContextProvider
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
class QuestionnaireUtilsTest : RobolectricTest() {
  private val iParser: IParser = FhirContext.forR4().newJsonParser()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun testExtractObservations() {
    val questionnaire = loadQuestionnaire(context, "sample_anc_service_enrollment.json")
    val questionnaireResponse =
      loadQuestionnaireResponse(context, "sample_anc_service_enrollment_questionnaireresponse.json")

    val target = mutableListOf<Observation>()
    val patient = Patient().apply { id = "test_patient_1_id" }

    QuestionnaireUtils.extractObservations(
      questionnaireResponse,
      questionnaire.item,
      patient,
      target
    )

    val gaObs = target[2]
    Assert.assertEquals(5, target.size)
    Assert.assertEquals("Fetal gestational age", gaObs.code.coding[0].display)
    Assert.assertEquals("57036006", gaObs.code.coding[0].code)
    Assert.assertEquals("Patient/test_patient_1_id", gaObs.subject.reference)
    Assert.assertEquals(5, gaObs.valueIntegerType.value)

    unmockkObject(SimpleWorkerContextProvider)
  }

  private fun loadQuestionnaire(context: Context, id: String): Questionnaire {
    val qJson = context.assets.open(id).bufferedReader().use { it.readText() }
    return iParser.parseResource(qJson) as Questionnaire
  }

  private fun loadQuestionnaireResponse(context: Context, id: String): QuestionnaireResponse {
    val qrJson = context.assets.open(id).bufferedReader().use { it.readText() }
    return iParser.parseResource(qrJson) as QuestionnaireResponse
  }
}
