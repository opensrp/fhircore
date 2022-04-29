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

package org.smartregister.fhircore.engine.data.local.patient.dao.register

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.helper.FhirMapperServices.parseRegisterMapping

@Singleton
class DefaultPatientRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
  val fhirPathEngine: FHIRPathEngine
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    return withContext(dispatcherProvider.io()) {
      getRegisterDataFilters()!!.let { param ->
        defaultRepository.loadRegisterListData(param, null, null).map { data ->
          parseRegisterMapping(param.name, data, configurationRegistry, fhirPathEngine)
        }
      }
    }
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long =
    withContext(dispatcherProvider.default()) {
      getRegisterDataFilters()?.let { defaultRepository.countDataForParam(it) } ?: 0
    }

  override suspend fun loadProfileData(appFeatureName: String?, id: String): ProfileData {
    val idPart = IdType(id).idPart
    return withContext(dispatcherProvider.io()) {
      getProfileDataFilters()!!
        .let { param -> defaultRepository.loadProfileData(param, null, StringType(idPart)) }
        .first() // TODO

      /* ProfileData.DefaultProfileData(
        id = patient.logicalId,
        name = patient.extractName(),
        identifier = patient.identifierFirstRep.value,
        address = patient.extractAge(),
        gender = patient.gender.toCode(),
        birthdate = patient.birthDate,
        deathDate =
          if (patient.hasDeceasedDateTimeType()) patient.deceasedDateTimeType.value else null,
        deceased =
          if (patient.hasDeceasedBooleanType()) patient.deceasedBooleanType.booleanValue()
          else null,
        visits =
          defaultRepository.searchResourceFor(
            subjectId = patientId,
            subjectParam = Encounter.SUBJECT
          ),
        flags =
          defaultRepository.searchResourceFor(subjectId = patientId, subjectParam = Flag.SUBJECT),
        conditions =
          defaultRepository.searchResourceFor(
            subjectId = patientId,
            subjectParam = Condition.SUBJECT
          ),
        tasks =
          defaultRepository.searchResourceFor(subjectId = patientId, subjectParam = Task.SUBJECT),
        services =
          defaultRepository.searchResourceFor(
            subjectId = patientId,
            subjectParam = CarePlan.SUBJECT
          ),
        forms = defaultRepository.searchQuestionnaireConfig(formsFilter),
        responses =
          defaultRepository.searchResourceFor(
            subjectId = patientId,
            subjectParam = QuestionnaireResponse.SUBJECT
          )
      )*/
    }
  }

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveRegisterDataFilterConfiguration(HealthModule.DEFAULT.name)

  private fun getProfileDataFilters() =
    configurationRegistry.retrieveProfileDataFilterConfiguration(HealthModule.DEFAULT.name)

  companion object {
    const val FORMS_LIST_FILTER_KEY = "forms_list"
  }
}
