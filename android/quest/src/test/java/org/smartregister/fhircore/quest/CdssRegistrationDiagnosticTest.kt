/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest

import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class CdssRegistrationDiagnosticTest : RobolectricTest() {

  private val worker =
    SimpleWorkerContext().apply {
      this.setExpansionProfile(Parameters())
      this.isCanRunWithoutTerminology = true
    }
  private val transformSupportServices = TransformSupportServices(worker)

  private fun basePath() =
    "${System.getProperty("user.dir")}/src/main/assets/configs/cdss/resources"

  private suspend fun extract(
    questionnaire: Questionnaire,
    structureMap: StructureMap,
    questionnaireResponse: QuestionnaireResponse,
  ) =
    ResourceMapper.extract(
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      structureMapExtractionContext =
        StructureMapExtractionContext(
          transformSupportServices = transformSupportServices,
          structureMapProvider = { _: String?, _: IWorkerContext -> structureMap },
        ),
    )

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun compileMapAndExtractAdultAndMinorWithGuardian() =
    runTest(timeout = kotlin.time.Duration.parse("90s")) {
      val questionnaireJson =
        java.io.File("${basePath()}/questionnaire/cdss-client-registration.json").readText()
      val mapText =
        java.io.File("${basePath()}/structuremap/cdss-client-registration.map").readText()

      val questionnaire = questionnaireJson.decodeResourceFromString<Questionnaire>()

      val compiled =
        StructureMapUtilities(worker, transformSupportServices)
          .parse(mapText, "CdssClientRegistration")
          .apply {
            id = "cdss-client-registration"
            url = "https://fhir.opensrp.io/cdss/StructureMap/cdss-client-registration"
            name = "CdssClientRegistration"
            title = "CDSS client registration"
            status = org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE
          }

      // Persist compiled StructureMap JSON next to the FML so assets/seed stay in sync.
      val compiledJson = compiled.encodeResourceToString()
      val outFile = java.io.File("${basePath()}/structuremap/cdss-client-registration.json")
      outFile.writeText(compiledJson + "\n")
      // Mirror under conf/fhir-seed and loose assets copy when present
      listOf(
          java.io.File(
            "${System.getProperty("user.dir")}/../../conf/fhir-seed/resources/StructureMap-cdss-client-registration.json",
          ),
          java.io.File(
            "${System.getProperty("user.dir")}/src/main/assets/resources/structuremap/cdss-client-registration.json",
          ),
        )
        .forEach { dest ->
          dest.parentFile?.mkdirs()
          if (dest.parentFile?.exists() == true) {
            dest.writeText(compiledJson + "\n")
          }
        }

      println("COMPILED STRUCTUREMAP written to ${outFile.absolutePath}")
      println("groups=${compiled.group.size} topRules=${compiled.groupFirstRep.rule.size}")

      // Adult, no guardian — must extract at least Patient.
      val adultResponse =
        """
        {
          "resourceType": "QuestionnaireResponse",
          "questionnaire": "https://fhir.opensrp.io/cdss/Questionnaire/cdss-client-registration",
          "status": "completed",
          "item": [
            {"linkId": "Ccc.A.DE01", "answer": [{"valueString": "ID-12345"}]},
            {"linkId": "Ccc.A.DE04", "answer": [{"valueString": "Jane"}]},
            {"linkId": "Ccc.A.DE05", "answer": [{"valueString": "M"}]},
            {"linkId": "Ccc.A.DE06", "answer": [{"valueString": "Doe"}]},
            {"linkId": "Ccc.A.DE08", "answer": [{"valueDate": "1990-01-15"}]},
            {"linkId": "Ccc.A.DE16", "answer": [{"valueCoding": {"code": "Ccc.A.DE17"}}]}
          ]
        }
        """
          .trimIndent()
          .decodeResourceFromString<QuestionnaireResponse>()

      val adultBundle = extract(questionnaire, compiled, adultResponse)
      println("ADULT ENTRY COUNT = ${adultBundle.entry.size}")
      println(adultBundle.encodeResourceToString())
      org.junit.Assert.assertEquals(
        1,
        adultBundle.entry.count { it.resource is org.hl7.fhir.r4.model.Patient },
      )
      org.junit.Assert.assertEquals(
        0,
        adultBundle.entry.count { it.resource is org.hl7.fhir.r4.model.RelatedPerson },
      )

      // Minor — registration extracts the child Patient only (no inline guardian).
      val minorResponse =
        """
        {
          "resourceType": "QuestionnaireResponse",
          "questionnaire": "https://fhir.opensrp.io/cdss/Questionnaire/cdss-client-registration",
          "status": "completed",
          "item": [
            {"linkId": "Ccc.A.DE01", "answer": [{"valueString": "ID-99999"}]},
            {"linkId": "Ccc.A.DE04", "answer": [{"valueString": "Timmy"}]},
            {"linkId": "Ccc.A.DE06", "answer": [{"valueString": "Doe"}]},
            {"linkId": "Ccc.A.DE08", "answer": [{"valueDate": "2020-05-01"}]},
            {"linkId": "Ccc.A.DE16", "answer": [{"valueCoding": {"code": "Ccc.A.DE18"}}]}
          ]
        }
        """
          .trimIndent()
          .decodeResourceFromString<QuestionnaireResponse>()

      val minorBundle = extract(questionnaire, compiled, minorResponse)
      println("MINOR ENTRY COUNT = ${minorBundle.entry.size}")
      println(minorBundle.encodeResourceToString())
      org.junit.Assert.assertEquals(
        1,
        minorBundle.entry.count { it.resource is org.hl7.fhir.r4.model.Patient },
      )
      org.junit.Assert.assertEquals(
        0,
        minorBundle.entry.count { it.resource is org.hl7.fhir.r4.model.RelatedPerson },
      )
    }
}
