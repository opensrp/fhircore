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

package org.smartregister.fhircore.anc.ui.family.details

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem

interface FamilyDetailDataProvider {

  fun getDemographics(): LiveData<Patient>
  fun getFamilyMembers(): LiveData<List<FamilyMemberItem>>
  fun getEncounters(): LiveData<List<Encounter>>
  fun getFamilyCarePlans(): LiveData<List<CarePlan>>

  fun getAppBackClickListener(): () -> Unit = {}
  fun getMemberItemClickListener(): (item: FamilyMemberItem) -> Unit = {}
  fun getAddMemberItemClickListener(): () -> Unit = {}
  fun getSeeAllEncounterClickListener(): () -> Unit = {}
  fun getEncounterItemClickListener(): (item: Encounter) -> Unit = {}
  fun getSeeAllUpcomingServiceClickListener(): () -> Unit = {}
  fun getUpcomingServiceItemClickListener(): (item: Task) -> Unit = {}
}
