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

package org.smartregister.fhircore.engine.ui.settings

import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.model.practitioner.PractitionerDetails

@ExcludeFromJacocoGeneratedReport
data class ProfileData(
  val userName: String,
  val locations: List<FieldData> = listOf(),
  val organisations: List<FieldData> = listOf(),
  val careTeams: List<FieldData> = listOf(),
  val isUserValid: Boolean,
  val practitionerDetails: PractitionerDetails?
)

@ExcludeFromJacocoGeneratedReport data class FieldData(val id: String, val value: String)
