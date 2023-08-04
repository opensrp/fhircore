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

package org.smartregister.fhircore.quest.data

import android.util.Log
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.QuestionnaireResponse
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

@Singleton
class QuestXFhirQueryResolver @Inject constructor(val fhirEngine: FhirEngine) : XFhirQueryResolver {
  override suspend fun resolve(xFhirQuery: String): List<Resource> {
    Log.d("FIKRI XFHIRQUERY", xFhirQuery)


    if (xFhirQuery.contains("QuestionnaireResponse")) {
      val strParams = xFhirQuery.removePrefix("QuestionnaireResponse?")
      val params = strParams.split("&")
      val search = Search(ResourceType.QuestionnaireResponse).apply {
//        filter(QuestionnaireResponse.SUBJECT, { value = "Patient/970fcf90-0383-45e1-acfa-1e3f0b7d0797" })
//        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/$questionnaireId" })
        params.forEach {
          val paramType = it.split("=").first()
          val paramValue = it.split("=").last()
          Log.d("FIKRI PARAM", "$paramType $paramValue")

          if (paramType == QuestionnaireResponse.SP_SUBJECT) {
            this.filter(QuestionnaireResponse.SUBJECT, { value = paramValue })
          }

//          if (paramType == QuestionnaireResponse.SP_PATIENT) {
//            Log.d("FIKRI PARAM 2", "$paramType $paramValue")
//            this.filter(QuestionnaireResponse.PATIENT, { value = paramValue })
//          }

          if (paramType == QuestionnaireResponse.SP_QUESTIONNAIRE) {
            this.filter(QuestionnaireResponse.QUESTIONNAIRE, { value = paramValue })
          }
        }
      }

//      val lists = fhirEngine.search<QuestionnaireResponse> {
//        params.forEach {
//          val paramType = it.split("=").first()
//          val paramValue = it.split("=").last()
//          Log.d("FIKRI PARAM", "$paramType $paramValue")
//
//          if (paramType == QuestionnaireResponse.SP_SUBJECT) {
//            Log.d("FIKRI PARAM 1", "$paramType $paramValue")
//            this.filter(QuestionnaireResponse.SUBJECT, { value = paramValue })
//          }
//
//          if (paramType == QuestionnaireResponse.SP_PATIENT) {
//            Log.d("FIKRI PARAM 2", "$paramType $paramValue")
//            this.filter(QuestionnaireResponse.PATIENT, { value = paramValue })
//          }
//
//          if (paramType == QuestionnaireResponse.SP_QUESTIONNAIRE) {
//            Log.d("FIKRI PARAM 3", "$paramType $paramValue")
//            this.filter(QuestionnaireResponse.QUESTIONNAIRE, { value = paramValue })
//          }
//        }
//        filter(QuestionnaireResponse.SUBJECT, { value = "Patient/970fcf90-0383-45e1-acfa-1e3f0b7d0797" })
//        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/$questionnaireId" })
//      }

      val lists = fhirEngine.search<QuestionnaireResponse>(search)
      Log.d("FIKRI TOTAL RES", lists.size.toString())

      return lists
    } else {
      return fhirEngine.search(xFhirQuery)
    }
  }
}
