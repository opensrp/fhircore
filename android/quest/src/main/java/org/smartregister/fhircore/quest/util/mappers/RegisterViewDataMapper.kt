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
import androidx.ui.text.substring
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Enumerations
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DueLightColor
import org.smartregister.fhircore.engine.ui.theme.FemalePinkColor
import org.smartregister.fhircore.engine.ui.theme.MaleBlueColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.OverdueLightColor
import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.engine.util.extension.translateGender
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.ui.shared.models.ServiceMember

class RegisterViewDataMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<RegisterData, RegisterViewData> {
  override fun transformInputToOutputModel(inputModel: RegisterData): RegisterViewData {
    return when (inputModel) {
      is RegisterData.DefaultRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = listOf(inputModel.name, inputModel.age).joinToString(", "),
          subtitle = inputModel.gender.translateGender(context).capitalizeFirstLetter(),
          registerType = RegisterData.DefaultRegisterData::class
        )
      is RegisterData.FamilyRegisterData -> {
        val serviceText =
          when {
            inputModel.servicesOverdue != 0 -> inputModel.servicesOverdue.toString()
            inputModel.servicesDue != 0 -> inputModel.servicesDue.toString()
            else -> null
          }
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = context.getString(R.string.family_suffix, inputModel.name),
          subtitle = inputModel.address,
          status = context.getString(R.string.date_last_visited, inputModel.lastSeen),
          serviceButtonActionable = false,
          serviceButtonBackgroundColor =
            if (inputModel.servicesOverdue != 0) OverdueDarkRedColor else Color.White,
          serviceButtonForegroundColor =
            if (inputModel.servicesOverdue != 0) Color.White else BlueTextColor,
          serviceMembers =
            inputModel.members.map {
              ServiceMember(
                icon =
                  when {
                    it.pregnant -> R.drawable.ic_pregnant
                    it.age.toInt() <= 5 -> R.drawable.ic_kids
                    else -> R.drawable.ic_users
                  },
                logicalId = it.logicalId
              )
            },
          serviceText = serviceText,
          borderedServiceButton = inputModel.servicesDue != 0 && inputModel.servicesOverdue == 0,
          serviceButtonBorderColor = BlueTextColor,
          showDivider = true,
          showServiceButton = !serviceText.isNullOrEmpty(),
          registerType = RegisterData.FamilyRegisterData::class
        )
      }
      is RegisterData.AncRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = listOf(inputModel.name, inputModel.age).joinToString(),
          status = inputModel.address,
          serviceButtonActionable = true,
          serviceButtonBackgroundColor =
            if (inputModel.servicesOverdue == 0) DueLightColor else OverdueLightColor,
          serviceButtonForegroundColor =
            if (inputModel.servicesOverdue == 0) BlueTextColor else OverdueDarkRedColor,
          serviceText = context.getString(R.string.anc_visit),
          showServiceButton = inputModel.servicesOverdue != 0 || inputModel.servicesDue != 0,
          registerType = RegisterData.AncRegisterData::class
        )
      is RegisterData.HivRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = inputModel.name,
          subtitle = "${inputModel.age}, ${inputModel.healthStatus.display}",
          registerType = RegisterData.HivRegisterData::class,
          identifier = inputModel.identifier?.let { if (it.length > 6) it.substring(0..5) else it }
              ?: "",
          serviceButtonBackgroundColor =
            if (inputModel.gender == Enumerations.AdministrativeGender.MALE) MaleBlueColor
            else FemalePinkColor,
          serviceTextIcon =
            when {
              inputModel.healthStatus == HealthStatus.EXPOSED_INFANT ->
                R.drawable.baseline_child_care_fill_48
              inputModel.gender == Enumerations.AdministrativeGender.MALE ->
                R.drawable.baseline_man_24
              inputModel.gender == Enumerations.AdministrativeGender.FEMALE ->
                R.drawable.baseline_woman_24
              else -> null
            }
        )
      is RegisterData.AppointmentRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = listOf(inputModel.name, inputModel.age).joinToString(", "),
          subtitle = inputModel.gender.translateGender(context).capitalizeFirstLetter(),
          registerType = RegisterData.AppointmentRegisterData::class
        )
      else -> throw UnsupportedOperationException()
    }
  }
}
