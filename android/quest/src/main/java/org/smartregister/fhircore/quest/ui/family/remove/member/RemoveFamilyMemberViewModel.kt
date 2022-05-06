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
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.quest.ui.family.remove.BaseRemoveFamilyEntityViewModel
import timber.log.Timber

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
        repository.registerDaoFactory.familyRegisterDao.removeFamilyMember(profileId, familyId)
        isRemoved.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
        isDiscarded.postValue(true)
      }
    }
  }

}
