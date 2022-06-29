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

package org.smartregister.fhircore.engine.data.local.register.dao

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.model.SearchFilter
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractName

@Singleton
class DefaultPatientRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider,
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(Patient.ACTIVE, { value = of(true) })
          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countRegisterData(appFeatureName).toInt() else DEFAULT_PAGE_SIZE
          from = currentPage * DEFAULT_PAGE_SIZE
        }

      patients.map {
        RegisterData.DefaultRegisterData(
          logicalId = it.logicalId,
          name = it.extractName(),
          gender = it.gender,
          age = it.extractAge()
        )
      }
    }

  override suspend fun countRegisterData(appFeatureName: String?): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData =
    withContext(dispatcherProvider.io()) {
      val patient = fhirEngine.get<Patient>(resourceId)
      val formsFilter = getRegisterDataFilters()

      ProfileData.DefaultProfileData(
        logicalId = patient.logicalId,
        name = patient.extractName(),
        identifier = patient.identifierFirstRep.value,
        address = patient.extractAge(),
        gender = patient.gender,
        birthdate = patient.birthDate,
        deathDate =
          if (patient.hasDeceasedDateTimeType()) patient.deceasedDateTimeType.value else null,
        deceased =
          if (patient.hasDeceasedBooleanType()) patient.deceasedBooleanType.booleanValue()
          else null,
        visits =
          defaultRepository.searchResourceFor(
            subjectId = resourceId,
            subjectParam = Encounter.SUBJECT
          ),
        flags =
          defaultRepository.searchResourceFor(subjectId = resourceId, subjectParam = Flag.SUBJECT),
        conditions =
          defaultRepository.searchResourceFor(
            subjectId = resourceId,
            subjectParam = Condition.SUBJECT
          ),
        tasks =
          defaultRepository.searchResourceFor<Task>(
              subjectId = resourceId,
              subjectParam = Task.SUBJECT
            )
            .sortedBy { it.executionPeriod.start.time },
        services =
          defaultRepository.searchResourceFor(
            subjectId = resourceId,
            subjectParam = CarePlan.SUBJECT
          ),
        forms = defaultRepository.searchQuestionnaireConfig(formsFilter),
        responses =
          defaultRepository.searchResourceFor(
            subjectId = resourceId,
            subjectParam = QuestionnaireResponse.SUBJECT
          )
      )
    }
  private fun getRegisterDataFilters() = emptyList<SearchFilter>()
  companion object {
    const val FORMS_LIST_FILTER_KEY = "forms_list"
  }
}
