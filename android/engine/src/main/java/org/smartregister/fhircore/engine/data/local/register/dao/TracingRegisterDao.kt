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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractFamilyName
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName

abstract class TracingRegisterDao
constructor(
  open val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DefaultDispatcherProvider
) : RegisterDao {
  protected abstract fun Search.registerFilters()

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {

    val tracingTasks =
      fhirEngine.search<Task> {
        registerFilters()
        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return tracingTasks.map { it.toTracingRegisterData() }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData? {
    return super.loadProfileData(appFeatureName, resourceId)
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine.count<Task> { registerFilters() }
  }

  private fun applicationConfiguration(): ApplicationConfiguration =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  private suspend fun Task.toTracingRegisterData(): RegisterData {
    val patient = defaultRepository.loadResource(this.`for`) as Patient

    return RegisterData.TracingRegisterData(
      logicalId = this.logicalId,
      name = patient.extractName(),
      identifier =
        this.identifier.firstOrNull { it.use == Identifier.IdentifierUse.OFFICIAL }?.value,
      gender = patient.gender,
      familyName = patient.extractFamilyName(),
      healthStatus =
        patient.extractHealthStatusFromMeta(
          applicationConfiguration().patientTypeFilterTagViaMetaCodingSystem
        ),
      isPregnant = defaultRepository.isPatientPregnant(patient),
      isBreastfeeding = defaultRepository.isPatientBreastfeeding(patient),
      attempts = this.relevantHistory.size,
      reasons =
        this.reasonCode.coding.map { it.display ?: it.code }.ifEmpty {
          listOf(this.reasonCode.text)
        },
    )
  }
}
