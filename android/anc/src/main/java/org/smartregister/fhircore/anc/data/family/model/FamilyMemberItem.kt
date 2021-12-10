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

package org.smartregister.fhircore.anc.data.family.model

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class FamilyMemberItem(
  val name: String,
  val id: String,
  val birthdate: Date?,
  val gender: String,
  val pregnant: Boolean? = null,
  val houseHoldHead: Boolean,
  val deathDate: Date? = null,
  val servicesDue: Int? = null,
  val servicesOverdue: Int? = null
)
