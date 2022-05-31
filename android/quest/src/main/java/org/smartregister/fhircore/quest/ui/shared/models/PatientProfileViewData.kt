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

package org.smartregister.fhircore.quest.ui.shared.models

import org.smartregister.fhircore.engine.domain.model.FormButtonData
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState

sealed class ProfileViewData(
  open val logicalId: String = "",
  open val name: String = "",
  open val identifier: String? = null,
) {
  data class PatientProfileViewData(
    override val logicalId: String = "",
    override val name: String = "",
    override val identifier: String? = null,
    val status: String? = null,
    val sex: String = "",
    val age: String = "",
    val dob: String = "",
    val tasks: List<PatientProfileRowItem> = emptyList(),
    val forms: List<FormButtonData> = emptyList(),
    val medicalHistoryData: List<PatientProfileRowItem> = emptyList(),
    val upcomingServices: List<PatientProfileRowItem> = emptyList(),
    val ancCardData: List<PatientProfileRowItem> = emptyList(),
  ) : ProfileViewData(name = name, logicalId = logicalId, identifier = identifier)

  data class FamilyProfileViewData(
    override val logicalId: String = "",
    override val name: String = "",
    val address: String = "",
    val age: String = "",
    val familyMemberViewStates: List<FamilyMemberViewState> = emptyList()
  ) : ProfileViewData(logicalId = logicalId, name = name)

  data class HivProfileViewData(
    override val logicalId: String = "",
    override val name: String = "",
    val address: String = "",
    val age: String = "",
    val artNumber: String = ""
  ) : ProfileViewData(logicalId = logicalId, name = name)

  data class AppointmentProfileViewData(
    override val logicalId: String = "",
    override val name: String = "",
    val address: String = "",
    val age: String = "",
    val artNumber: String = ""
  ) : ProfileViewData(logicalId = logicalId, name = name)
}
