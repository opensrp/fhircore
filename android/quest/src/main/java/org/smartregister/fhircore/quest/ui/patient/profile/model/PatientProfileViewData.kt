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

package org.smartregister.fhircore.quest.ui.patient.profile.model

import org.smartregister.fhircore.engine.domain.model.FormButtonData
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState

sealed class ProfileViewData(open val id: String = "", open val name: String = "") {
  data class PatientProfileViewData(
    override val id: String = "",
    override val name: String = "",
    val status: String? = null,
    val sex: String = "",
    val age: String = "",
    val dob: String = "",
    val tasks: List<PatientProfileRowItem> = emptyList(),
    val forms: List<FormButtonData> = emptyList(),
    val medicalHistoryData: List<PatientProfileRowItem> = emptyList(),
    val upcomingServices: List<PatientProfileRowItem> = emptyList(),
    val ancCardData: List<PatientProfileRowItem> = emptyList()
  ) : ProfileViewData(name = name, id = id)

  data class FamilyProfileViewData(
    override val id: String = "",
    override val name: String = "",
    val address: String = "",
    val familyMemberViewStates: List<FamilyMemberViewState> = emptyList()
  ) : ProfileViewData(id = id, name = name)
}
