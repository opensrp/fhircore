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

package org.smartregister.fhircore.quest.ui.profile

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.models.PatientProfileViewSection
import org.smartregister.fhircore.quest.ui.shared.models.ViewComponentEvent

sealed class ProfileEvent {

  data class SeeAll(val patientProfileViewSection: PatientProfileViewSection) : ProfileEvent()

  data class LoadQuestionnaire(val questionnaireId: String, val context: Context) : ProfileEvent()

  data class OpenTaskForm(
    val context: Context,
    val taskFormId: String,
    val taskId: String?,
    val patientId: String
  ) : ProfileEvent()

  data class OverflowMenuClick(
    val navController: NavHostController,
    val context: Context,
    val menuId: Int,
    val resourceData: ResourceData?,
    val profileConfiguration: ProfileConfiguration?
  ) : ProfileEvent()

  data class OnViewComponentEvent(
    val viewComponentEvent: ViewComponentEvent,
    val navController: NavController
  ) : ProfileEvent()
}
