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
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asCodeableConcept
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asReference
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.filterByPatientName
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractFamilyTag
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
          filter(Patient.ACTIVE, { value = of(true) })
          filterByPatientName(query)

          sort(Patient.NAME, Order.ASCENDING)
          count = if (loadAll) countAll().toInt() else PaginationUtil.DEFAULT_PAGE_SIZE
          from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map { p ->
        val members = searchFamilyMembers(p.logicalId)

        val familyServices = ancPatientRepository.searchCarePlan(p.logicalId, p.extractFamilyTag())
        domainMapper.mapToDomainModel(Family(p, members, familyServices))
      }
    }
  }

  override suspend fun countAll(): Long {
    return fhirEngine.count<Patient> {
      filterBy(registerConfig.primaryFilter!!)
      filter(Patient.ACTIVE, { value = of(true) })
    }
  }

  suspend fun searchFamilyMembers(familyHeadId: String): List<FamilyMemberItem> {
    return ancPatientRepository
      .searchPatientByLink(familyHeadId)
      .plus(fhirEngine.load(Patient::class.java, familyHeadId))
      .map {
        val services = ancPatientRepository.searchCarePlan(it.logicalId)
        val conditions = ancPatientRepository.searchCondition(it.logicalId)
        domainMapper.toFamilyMemberItem(it, conditions, services)
      }
      .sortedBy {
        var weight = 0
        if (!it.houseHoldHead) weight++ // 0 for HH
        if (it.deathDate != null) weight++ // 0 for alive
        weight
      }
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

  /**
   * - Assign family tag to new head
   * - Remove family tag from older head
   * - Remove old head link from new head
   * - Assign new head reference to old head
   * - Assign new head reference to all members
   * - Assign address to new head
   * - Assign family care plans to new head
   * - Add Family Flag for new head
   * - Mark old Flag as inactive
   */
  suspend fun changeFamilyHead(currentHeadId: String, newHeadId: String) =
    withContext(dispatcherProvider.io()) {
      if (currentHeadId == newHeadId)
        throw IllegalStateException("Current and new Head ids are same")

      val currentHead = fhirEngine.load(Patient::class.java, currentHeadId)
      val newHead = fhirEngine.load(Patient::class.java, newHeadId)

      if (!newHead.active || newHead.hasDeceased())
        throw IllegalStateException("Inactive or deceased person can not be new head of family")

      val familyTag = currentHead.extractFamilyTag()!!
      val familyExt =
        currentHead.extension.singleOrNull { it.value.toString().contentEquals(familyTag.display) }

      newHead.meta.addTag(familyTag)
      newHead.extension.add(familyExt)
      newHead.address = currentHead.address
      newHead.link.clear()

      val newHeadFlag = Flag()
      newHeadFlag.id = getUniqueId()
      newHeadFlag.status = Flag.FlagStatus.ACTIVE
      newHeadFlag.subject = newHead.asReference()
      newHeadFlag.code = familyTag.asCodeableConcept()
      newHeadFlag.period.start = Date()

      fhirEngine.save(newHeadFlag)
      fhirEngine.save(newHead)

      ancPatientRepository.searchCarePlan(currentHeadId, familyTag).forEach {
        // assign family care plan to new head
        it.subject = newHead.asReference()

        fhirEngine.save(it)
      }

      currentHead.meta.tag.remove(familyTag)
      currentHead.extension.remove(familyExt)
      currentHead.addLink().apply {
        this.other = newHead.asReference()
        this.type = Patient.LinkType.REFER
      }

      ancPatientRepository.fetchActiveFlag(currentHeadId, familyTag)?.run {
        this.status = Flag.FlagStatus.INACTIVE
        this.period.end = Date()

        fhirEngine.save(this)
      }

      fhirEngine.save(currentHead)

      searchFamilyMembers(currentHeadId)
        .filter { it.id != currentHeadId && it.id != newHeadId }
        .forEach {
          val member = fhirEngine.load(Patient::class.java, it.id)
          member.linkFirstRep.other = newHead.asReference()

          fhirEngine.save(member)
        }
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
