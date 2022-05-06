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
import javax.inject.Inject
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.model.VisitStatus
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DueLightColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.OverdueLightColor
import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.engine.util.extension.milestonesDue
import org.smartregister.fhircore.engine.util.extension.milestonesOverdue
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.engine.util.extension.translateGender
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

class RegisterViewDataMapper @Inject constructor(@ApplicationContext val context: Context) :
  DataMapper<RegisterData, RegisterViewData> {
  override fun transformInputToOutputModel(inputModel: RegisterData): RegisterViewData {
    return when (inputModel) {
      is RegisterData.DefaultRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = listOf(inputModel.name, inputModel.birthDate.toAgeDisplay()).joinToString(", "),
          subtitle = inputModel.gender.translateGender(context).capitalizeFirstLetter()
        )
      is RegisterData.FamilyRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = context.getString(R.string.family_suffix, inputModel.name),
          subtitle = inputModel.address,
          status = context.getString(R.string.date_last_visited, inputModel.lastSeen),
          serviceButtonActionable = false,
          serviceButtonBackgroundColor =
            if (inputModel.services.any { it.milestonesOverdue().isNotEmpty() }) OverdueDarkRedColor
            else Color.White,
          serviceButtonForegroundColor =
            if (inputModel.services.any { it.milestonesOverdue().isNotEmpty() }) Color.White
            else BlueTextColor,
          serviceText =
            when {
              inputModel.services.any { it.milestonesOverdue().isNotEmpty() } ->
                inputModel.services.sumOf { it.milestonesOverdue().size }.toString()
              inputModel.services.any { it.milestonesDue().isNotEmpty() } ->
                inputModel.services.sumOf { it.milestonesDue().size }.toString()
              else -> null
            },
          borderedServiceButton = inputModel.services.any { it.milestonesOverdue().size > 0 },
          serviceButtonBorderColor = BlueTextColor,
          showDivider = true,
          showServiceButton = inputModel.services.isNotEmpty()
        )
      is RegisterData.AncRegisterData ->
        RegisterViewData(
          logicalId = inputModel.logicalId,
          title = listOf(inputModel.name, inputModel.birthDate.toAgeDisplay()).joinToString(),
          subtitle = inputModel.address,
          status =
            inputModel
              .let {
                if (it.services.any { it.milestonesOverdue().isNotEmpty() }) VisitStatus.OVERDUE
                else if (it.services.any { it.milestonesDue().isNotEmpty() }) VisitStatus.DUE
                else VisitStatus.PLANNED
              }
              .name,
          serviceButtonActionable = true,
          serviceButtonBackgroundColor =
            if (inputModel.services.count { it.milestonesOverdue().isEmpty() } == 0) DueLightColor
            else OverdueLightColor,
          serviceButtonForegroundColor =
            if (inputModel.services.count { it.milestonesOverdue().isEmpty() } == 0) BlueTextColor
            else OverdueDarkRedColor,
          serviceText = context.getString(R.string.anc_visit),
          showServiceButton = inputModel.services.isNotEmpty()
        )
      else -> throw UnsupportedOperationException()
    }
  }
}
