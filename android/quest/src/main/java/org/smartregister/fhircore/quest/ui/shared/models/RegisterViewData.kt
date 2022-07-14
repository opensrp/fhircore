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

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.reflect.KClass
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor

@Stable
data class RegisterViewData(
  val logicalId: String,
  val title: String,
  val subtitle: String? = null,
  val status: String? = null,
  val otherStatus: String? = null,
  val serviceMembers: List<ServiceMember> = emptyList(),
  val serviceText: String? = null,
  @DrawableRes val serviceTextIcon: Int? = null,
  val serviceButtonActionable: Boolean = false,
  val serviceButtonForegroundColor: Color = DefaultColor,
  val serviceButtonBackgroundColor: Color = Color.White,
  val serviceButtonBorderColor: Color = InfoColor,
  val borderedServiceButton: Boolean = false,
  val showDivider: Boolean = false,
  val showServiceButton: Boolean = true,
  val registerType: KClass<out RegisterData> = RegisterData.DefaultRegisterData::class,
  val identifier: String = ""
)
