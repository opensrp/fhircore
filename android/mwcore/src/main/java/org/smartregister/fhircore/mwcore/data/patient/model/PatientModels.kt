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

package org.smartregister.fhircore.mwcore.data.patient.model

import androidx.compose.runtime.Stable
import java.util.Date
import org.smartregister.fhircore.mwcore.configuration.view.Properties

@Stable
data class PatientItem(
  val id: String = "",
  val identifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val address: String = "",
  var additionalData: List<AdditionalData>? = null
)

@Stable
data class AdditionalData(
  val label: String? = null,
  val value: String,
  var valuePrefix: String? = null,
  var valuePostfix: String? = null,
  var lastDateAdded: String? = null,
  val properties: Properties? = null
)

fun PatientItem.genderFull(): String {
  return when (gender) {
    "M" -> "Male"
    "F" -> "Female"
    else -> gender
  }
}

@Stable
data class QuestResultItem(
  val source: Pair<QuestionnaireResponseItem, QuestionnaireItem>,
  val data: List<List<AdditionalData>>
)

data class QuestionnaireResponseItem(
  val logicalId: String,
  val authored: Date? = null,
  val encounterId: String?,
  val questionnaireResponseString: String
)

data class QuestionnaireItem(
  val logicalId: String? = null,
  val name: String? = null,
  val title: String? = null
)
