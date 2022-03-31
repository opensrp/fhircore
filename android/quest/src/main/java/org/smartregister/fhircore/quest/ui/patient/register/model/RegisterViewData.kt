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

package org.smartregister.fhircore.quest.ui.patient.register.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.ui.theme.DefaultColor

@Stable
data class RegisterViewData(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val status: String? = null,
  val otherStatus: String? = null,
  val serviceAsButton: Boolean = false,
  val serviceMemberIcons: List<Int>? = emptyList(),
  val serviceText: String? = null,
  val serviceTextIcon: Int? = null,
  val serviceForegroundColor: Color = DefaultColor,
  val serviceBackgroundColor: Color = Color.White,
  val healthModule: HealthModule? = null
)
