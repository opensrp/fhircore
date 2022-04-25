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

package org.smartregister.fhircore.engine.data.local

import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadPatientImmunizations
import org.smartregister.fhircore.engine.util.extension.loadRelatedPersons
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.updateFrom

@Singleton
open class DefaultRepository
@Inject
constructor(open val fhirEngine: FhirEngine, open val dispatcherProvider: DispatcherProvider) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(resourceId) }
  }

  suspend fun loadRelatedPersons(patientId: String): List<RelatedPerson>? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadRelatedPersons(patientId) }
  }

  suspend fun loadPatientImmunizations(patientId: String): List<Immunization>? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadPatientImmunizations(patientId) }
  }

  suspend fun loadImmunization(immunizationId: String): Immunization? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(immunizationId) }
  }

  suspend fun loadQuestionnaireResponses(patientId: String, questionnaire: Questionnaire) =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search<QuestionnaireResponse> {
        filter(QuestionnaireResponse.SUBJECT, { value = "Patient/$patientId" })
        filter(QuestionnaireResponse.QUESTIONNAIRE, { value = "Questionnaire/${questionnaire.id}" })
      }
    }

  suspend fun search(dataRequirement: DataRequirement) =
    when (dataRequirement.type) {
      Enumerations.ResourceType.CONDITION.toCode() ->
        fhirEngine.search<Condition> {
          dataRequirement.codeFilter.forEach {
            filter(TokenClientParam(it.path), { value = of(it.codeFirstRep) })
          }
          // TODO handle date filter
        }
      else -> listOf()
    }

  suspend fun save(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      resource.generateMissingId()
      fhirEngine.save(resource)
    }
  }

  suspend fun delete(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      fhirEngine.remove(resource::class.java, resource.logicalId)
    }
  }

  suspend fun <R : Resource> addOrUpdate(resource: R) {
    return withContext(dispatcherProvider.io()) {
      try {
        fhirEngine.load(resource::class.java, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        resource.generateMissingId()
        fhirEngine.save(resource)
      }
    }
  }

}
