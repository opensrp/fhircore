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

package org.smartregister.fhircore.quest.data.patient.model

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import org.smartregister.fhircore.quest.R

@Stable
data class PatientItem(
  val id: String = "",
  val identifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val address: String = "",
  val g6pdStatus: G6PDStatus? = null
)

fun PatientItem.genderFull(): String {
  return when (gender) {
    "M" -> "Male"
    "F" -> "Female"
    else -> gender
  }
}

enum class G6PDStatus {
  Deficient(Color.Red),
  Intermediate(Color(android.graphics.Color.parseColor("#FFA500"))),
  Normal(Color.Green);

  val color: Color

  constructor(color: Color) {
    this.color = color
  }
}

fun G6PDStatus.makeItLabel(context: Context): String =
  context.getString(R.string.g6pd_status_label, name)
