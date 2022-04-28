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

package org.smartregister.fhircore.quest.ui.family.remove.member

import androidx.lifecycle.viewModelScope
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.quest.ui.family.remove.BaseRemoveFamilyEntityViewModel
import timber.log.Timber
import java.util.*

@HiltViewModel
class RemoveFamilyMemberViewModel
@Inject
constructor(
  override val repository: PatientRegisterRepository,
  val appFeatureManager: AppFeatureManager
) : BaseRemoveFamilyEntityViewModel<Patient>(repository) {

  override fun fetch(profileId: String) {
    viewModelScope.launch { profile.postValue(repository.loadResource(profileId)) }
  }

  override fun remove(profileId: String, familyId: String?) {
    viewModelScope.launch {
      try {
        repository.loadResource<Patient>(profileId)?.let { patient ->
          if (!patient.active) throw IllegalStateException("Patient already deleted")
          patient.active = false

          if (familyId != null) {
            repository.loadResource<Group>(familyId)?.let { family ->
              family.member.run {
                remove(this.find { it.entity.reference == "Patient/${patient.logicalId}" })
              }

              //TODO fetch head and compare to patient.. make   family.managingEntity ref to null

            }
          }

          repository.addOrUpdate(patient)
        }
        isRemoved.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
        isDiscarded.postValue(true)
      }
    }
  }

  private suspend fun loadFamilyHead(family: Group) =
    family.managingEntity?.let { reference ->
      repository
        .searchResourceFor<RelatedPerson>(
          token = RelatedPerson.RES_ID,
          subjectType = ResourceType.RelatedPerson,
          subjectId = reference.extractId()
        )
        .firstOrNull()
        ?.let { relatedPerson ->
          repository
            .searchResourceFor<Patient>(
              token = Patient.RES_ID,
              subjectType = ResourceType.Patient,
              subjectId = relatedPerson.patient.extractId()
            )
            .firstOrNull()
        }
    }

}