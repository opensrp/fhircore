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

package org.smartregister.fhircore.quest.util.mappers

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.domain.model.ActionableButtonData
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.hasStarted
import org.smartregister.fhircore.engine.util.extension.prettifyDate
import org.smartregister.fhircore.engine.util.extension.translateGender
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberTask
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData

class ProfileViewDataMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<ProfileData, ProfileViewData> {

  override fun transformInputToOutputModel(inputModel: ProfileData): ProfileViewData {
    return when (inputModel) {
      is ProfileData.AncProfileData ->
        ProfileViewData.PatientProfileViewData(
          logicalId = inputModel.logicalId,
          name = inputModel.name,
          sex = inputModel.gender.translateGender(context),
          age = inputModel.age,
          dob = inputModel.birthdate,
          identifier = inputModel.identifier
        )
      is ProfileData.HivProfileData ->
        ProfileViewData.PatientProfileViewData(
          logicalId = inputModel.logicalId,
          name = inputModel.name,
          sex = inputModel.gender.translateGender(context),
          age = inputModel.age,
          dob = inputModel.birthdate,
          identifier = inputModel.identifier,
          address = inputModel.address,
          identifierKey = inputModel.healthStatus.retrieveDisplayIdentifierKey(),
          showIdentifierInProfile = inputModel.showIdentifierInProfile,
          showListsHighlights = false,
          tasks =
            inputModel.tasks.sortedWith(compareBy<Task> { it.description }).map {
              ActionableButtonData(
                action = it.description,
                questionnaireId =
                  if (it.status == Task.TaskStatus.READY && it.hasReasonReference())
                    it.reasonReference.extractId()
                  else null,
                backReference = it.logicalId.asReference(ResourceType.Task),
                contentColor = it.status.retrieveColorCode(it.hasStarted()),
                iconStart =
                  if (it.status == Task.TaskStatus.COMPLETED) Icons.Filled.Check
                  else Icons.Filled.Add,
                iconColor =
                  it.status.retrieveColorCode(
                    hasStarted = it.hasStarted(),
                    changeCompleteStatusColor = true
                  ),
              )
            }
        )
      is ProfileData.DefaultProfileData ->
        ProfileViewData.PatientProfileViewData(
          logicalId = inputModel.logicalId,
          name = inputModel.name,
          identifier = inputModel.identifier,
          age = inputModel.age,
          sex = inputModel.gender.translateGender(context),
          dob = inputModel.birthdate,
          tasks =
            inputModel.tasks.take(DEFAULT_TASKS_COUNT).map {
              ActionableButtonData(
                action =
                  when (it.status) {
                    Task.TaskStatus.CANCELLED, Task.TaskStatus.FAILED ->
                      context.getString(
                        R.string.visit_overdue,
                        it.description.capitalizeFirstLetter(),
                        it.executionPeriod.start.prettifyDate()
                      )
                    Task.TaskStatus.READY ->
                      context.getString(
                        R.string.visit_due_today,
                        it.description.capitalizeFirstLetter()
                      )
                    Task.TaskStatus.COMPLETED -> it.description.capitalizeFirstLetter()
                    else ->
                      context.getString(
                        R.string.visit_due_on,
                        it.description.capitalizeFirstLetter(),
                        it.executionPeriod.start.prettifyDate()
                      )
                  },
                questionnaireId =
                  if (it.status == Task.TaskStatus.READY && it.hasReasonReference())
                    it.reasonReference.extractId()
                  else null,
                backReference = it.logicalId.asReference(ResourceType.Task),
                contentColor = it.status.retrieveColorCode(it.hasStarted()),
                iconStart =
                  if (it.status == Task.TaskStatus.COMPLETED) Icons.Filled.Check
                  else Icons.Filled.Add,
                iconColor =
                  it.status.retrieveColorCode(
                    hasStarted = it.hasStarted(),
                    changeCompleteStatusColor = true
                  ),
              )
            },
        )
      is ProfileData.FamilyProfileData ->
        ProfileViewData.FamilyProfileViewData(
          logicalId = inputModel.logicalId,
          name = context.getString(R.string.family_suffix, inputModel.name),
          address = inputModel.address,
          age = inputModel.age,
          familyMemberViewStates =
            inputModel.members.map { memberProfileData ->
              FamilyMemberViewState(
                patientId = memberProfileData.id,
                birthDate = memberProfileData.birthdate,
                age = memberProfileData.age,
                gender = memberProfileData.gender.translateGender(context),
                name = memberProfileData.name,
                memberTasks =
                  memberProfileData
                    .tasks
                    .filter { it.status == Task.TaskStatus.READY }
                    .take(DEFAULT_TASKS_COUNT)
                    .map {
                      FamilyMemberTask(
                        taskId = it.logicalId,
                        task = it.description,
                        taskStatus = it.status,
                        colorCode = it.status.retrieveColorCode(it.hasStarted()),
                        taskFormId =
                          if (it.status == Task.TaskStatus.READY &&
                              it.hasStarted() &&
                              it.hasReasonReference()
                          )
                            it.reasonReference.extractId()
                          else null
                      )
                    }
              )
            }
        )
    }
  }

  private fun Task.TaskStatus.retrieveColorCode(
    hasStarted: Boolean,
    changeCompleteStatusColor: Boolean = false
  ): Color =
    when (this) {
      Task.TaskStatus.READY -> if (hasStarted) InfoColor else DefaultColor
      Task.TaskStatus.CANCELLED -> DefaultColor
      Task.TaskStatus.FAILED -> OverdueColor
      Task.TaskStatus.COMPLETED -> if (changeCompleteStatusColor) SuccessColor else DefaultColor
      else -> DefaultColor
    }

  fun HealthStatus.retrieveDisplayIdentifierKey(): String =
    when (this) {
      HealthStatus.EXPOSED_INFANT -> "HCC Number"
      HealthStatus.CHILD_CONTACT, HealthStatus.SEXUAL_CONTACT, HealthStatus.COMMUNITY_POSITIVE ->
        "HTS Number"
      else -> "ART Number"
    }

  companion object {
    const val DEFAULT_TASKS_COUNT = 5 // TODO Configure tasks to display
  }
}
