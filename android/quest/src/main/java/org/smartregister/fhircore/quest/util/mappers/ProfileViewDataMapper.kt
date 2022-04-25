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
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.TaskStatus
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.translateGender
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberTask
import org.smartregister.fhircore.quest.ui.family.profile.model.FamilyMemberViewState
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData

class ProfileViewDataMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<ProfileData, ProfileViewData> {

  private val simpleDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

  override fun transformInputToOutputModel(inputModel: ProfileData): ProfileViewData {
    return when (inputModel) {
      is ProfileData.AncProfileData ->
        ProfileViewData.PatientProfileViewData(
          logicalId = inputModel.logicalId,
          name = inputModel.name,
          sex = inputModel.gender.translateGender(context),
          age = inputModel.age,
          dob = inputModel.birthdate.formatDob(),
          identifier = inputModel.identifier
        )
      is ProfileData.DefaultProfileData ->
        ProfileViewData.PatientProfileViewData(
          logicalId = inputModel.logicalId,
          name = inputModel.name,
          identifier = inputModel.identifier,
          age = inputModel.age,
          sex = inputModel.gender.translateGender(context),
          dob = inputModel.birthdate.formatDob()
        )
      is ProfileData.FamilyProfileData ->
        ProfileViewData.FamilyProfileViewData(
          logicalId = inputModel.logicalId,
          name = context.getString(R.string.family_suffix, inputModel.name),
          address = inputModel.address,
          familyMemberViewStates =
            inputModel.members.map { memberProfileData ->
              FamilyMemberViewState(
                patientId = memberProfileData.id,
                age = memberProfileData.age,
                gender = memberProfileData.gender.translateGender(context),
                name = memberProfileData.name,
                memberTasks =
                  memberProfileData.tasks.map {
                    FamilyMemberTask(
                      task = it.description,
                      taskStatus = TaskStatus.DUE,
                      colorCode = Color.Unspecified
                    )
                  }
              )
            }
        )
    }
  }

  private fun Date.formatDob(): String = simpleDateFormat.format(this)
}
