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

package org.smartregister.fhircore.quest.ui.family.profile.model

import java.util.Date

data class FamilyMemberViewState(
  val patientId: String,
  val name: String,
  val gender: String,
  val birthDate: Date? = null,
  val age: String,
  val statuses: List<String> = emptyList(),
  val showAtRisk: Boolean = false,
  val isDeceased: Boolean = false,
  val memberIcon: Int? = null,
  val memberTasks: List<FamilyMemberTask> = emptyList()
)

data class EligibleFamilyHeadMember(
  val list: List<EligibleFamilyHeadMemberViewState>,
  var reselect: Boolean = false
)

data class EligibleFamilyHeadMemberViewState(
  val familyMember: FamilyMemberViewState,
  var selected: Boolean = false
)
