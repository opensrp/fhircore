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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.anc

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.filterBy

@Singleton
class AncPatientRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      val pregnancies =
        fhirEngine
          .search<Condition> {
            getRegisterDataFilters().forEach { filterBy(it) }

            count =
              if (loadAll) countRegisterData(appFeatureName).toInt()
              else PaginationConstant.DEFAULT_PAGE_SIZE
            from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
          }
          .distinctBy { it.subject.reference }

      val patients =
        pregnancies.map { fhirEngine.load(Patient::class.java, it.subject.extractId()) }.sortedBy {
          it.nameFirstRep.family
        }

      patients.map {
        val carePlans =
          defaultRepository.searchResourceFor<CarePlan>(
            subjectId = it.logicalId,
            subjectParam = CarePlan.SUBJECT
          )
        val conditions =
          defaultRepository.searchResourceFor<Condition>(
            subjectId = it.logicalId,
            subjectParam = Condition.SUBJECT
          )

        AncRegisterDataMapper.transformInputToOutputModel(Anc(it, conditions, carePlans))
      }
    }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return withContext(dispatcherProvider.io()) {
      fhirEngine.count<Condition> { getRegisterDataFilters().forEach { filterBy(it) } }
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    val patient = defaultRepository.loadResource<Patient>(resourceId)!!
    val carePlans =
      defaultRepository.searchResourceFor<CarePlan>(
        subjectId = patient.logicalId,
        subjectParam = CarePlan.SUBJECT
      )
    val conditions =
      defaultRepository.searchResourceFor<Condition>(
        subjectId = patient.logicalId,
        subjectParam = Condition.SUBJECT
      )

    return AncProfileDataMapper.transformInputToOutputModel(
      AncProfile(patient, conditions, carePlans)
    )
  }

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.ANC.name)
}
