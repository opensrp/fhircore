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

package org.smartregister.fhircore.quest.ui.family.remove.family

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.quest.ui.family.remove.BaseRemoveFamilyEntityViewModel
import timber.log.Timber

@HiltViewModel
class RemoveFamilyViewModel
@Inject
constructor(
  override val repository: PatientRegisterRepository,
  val appFeatureManager: AppFeatureManager
) : BaseRemoveFamilyEntityViewModel<Group>(repository) {

  var isDeactivateMembers = false

  init {
    isDeactivateMembers =
      appFeatureManager.appFeatureSettings(AppFeature.HouseholdManagement)[
          DEACTIVATE_FAMILY_MEMBERS_SETTING_KEY]
        .toBoolean()
  }

  override fun fetch(profileId: String) {
    viewModelScope.launch { profile.postValue(repository.loadResource(profileId)) }
  }

  override fun remove(profileId: String, familyId: String?) {
    viewModelScope.launch {
      try {
        repository.loadResource<Group>(profileId)?.let { family ->
          if (!family.active) throw IllegalStateException("Family already deleted")
          family
            .managingEntity
            ?.let { reference ->
              repository.searchResourceFor<RelatedPerson>(
                token = RelatedPerson.RES_ID,
                subjectType = ResourceType.RelatedPerson,
                subjectId = reference.extractId()
              )
            }
            ?.firstOrNull()
            ?.let { relatedPerson -> repository.delete(relatedPerson) }
          family.managingEntity = null
          isDeactivateMembers.let {
            if (it) {
              family.member.map { member ->
                repository.loadResource<Patient>(member.entity.extractId())?.let { patient ->
                  patient.active = false
                  repository.addOrUpdate(patient)
                }
              }
            }
          }
          family.member.clear()
          family.active = false

          repository.addOrUpdate(family)
        }
        isRemoved.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
        isDiscarded.postValue(true)
      }
    }
  }

  companion object {
    const val DEACTIVATE_FAMILY_MEMBERS_SETTING_KEY = "deactivateMembers"
  }
}
