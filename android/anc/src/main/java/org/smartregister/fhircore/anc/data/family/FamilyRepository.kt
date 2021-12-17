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

package org.smartregister.fhircore.anc.data.family

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.filterByPatient
import org.smartregister.fhircore.anc.util.filterByPatientName
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.find

class FamilyRepository
@Inject
constructor(
  @ApplicationContext val context: Context,
  override val fhirEngine: FhirEngine,
  override val domainMapper: FamilyItemMapper,
  val dispatcherProvider: DispatcherProvider,
  val ancPatientRepository: PatientRepository
) : RegisterRepository<Family, FamilyItem> {

  private val registerConfig = context.loadRegisterConfig(RegisterType.FAMILY_REGISTER_ID)

  private val detailRepository = DefaultRepository(fhirEngine, dispatcherProvider)

  private val resourceMapperExtended = ResourceMapperExtended(detailRepository)

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<FamilyItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filterBy(registerConfig.primaryFilter!!)

          filterByPatientName(query)

          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map { p ->
        val carePlans = ancPatientRepository.searchCarePlan(p.logicalId).toMutableList()
        val members = fhirEngine.search<Patient> { filterByPatient(Patient.LINK, p.logicalId) }

        members.forEach { carePlans.addAll(ancPatientRepository.searchCarePlan(it.logicalId)) }

        domainMapper.mapToDomainModel(Family(p, members, carePlans))
      }
    }
  }

  override suspend fun countAll(): Long {
    return fhirEngine.count<Patient> { filterBy(registerConfig.primaryFilter!!) }
  }

  suspend fun postProcessFamilyMember(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    relatedTo: String?
  ): String {
    val patientId = getUniqueId()
    resourceMapperExtended.saveParsedResource(
      questionnaireResponse,
      questionnaire,
      patientId,
      relatedTo
    )

    return patientId
  }

  suspend fun postProcessFamilyHead(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ): String {
    return postProcessFamilyMember(questionnaire, questionnaireResponse, null)
  }

  suspend fun updateProcessFamilyHead(
    patientId: String,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    updateProcessFamilyMember(patientId, questionnaire, questionnaireResponse, null)
  }

  suspend fun updateProcessFamilyMember(
    patientId: String,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    relatedTo: String?
  ) {
    resourceMapperExtended.saveParsedResource(
      questionnaireResponse,
      questionnaire,
      patientId,
      relatedTo,
      editForm = true
    )
  }

  suspend fun enrollIntoAnc(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientId: String
  ) {
    resourceMapperExtended.saveParsedResource(questionnaireResponse, questionnaire, patientId, null)

    val lmpItem = questionnaireResponse.find(LMP_KEY)
    val lmp = lmpItem?.answer?.firstOrNull()?.valueDateType!!
    ancPatientRepository.enrollIntoAnc(patientId, lmp)
  }

  companion object {
    const val LMP_KEY = "lmp"
  }
}
