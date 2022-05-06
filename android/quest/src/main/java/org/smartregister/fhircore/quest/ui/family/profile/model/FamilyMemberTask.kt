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

import androidx.compose.ui.graphics.Color
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.TaskStatus

data class FamilyMemberTask(
  val taskFormId: String? = null,
  val task: String,
  val taskStatus: Task.TaskStatus,
  val colorCode: Color,
)
