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

import javax.inject.Inject
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DueLightColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.OverdueLightColor
import org.smartregister.fhircore.quest.ui.patient.register.model.RegisterViewData

class RegisterViewDataMapper @Inject constructor() : DataMapper<RegisterData, RegisterViewData> {
  override fun transformInputToOutputModel(inputModel: RegisterData): RegisterViewData {
    return when (inputModel) {
      is RegisterData.DefaultRegisterData ->
        RegisterViewData(
          id = inputModel.id,
          title = listOf(inputModel.name, inputModel.age).joinToString(", "),
          subtitle =
            inputModel.gender.name.lowercase().replaceFirstChar {
              it.uppercase()
            } // TODO make transalatable
        )
      is RegisterData.FamilyRegisterData ->
        RegisterViewData(
          id = inputModel.id,
          title = listOf(inputModel.name, inputModel.address).joinToString(),
          subtitle = inputModel.address,
          healthModule = HealthModule.FAMILY,
          status = "", // tODO
          otherStatus = "", // TODO
          serviceAsButton = true,
          serviceBackgroundColor =
            if (inputModel.servicesOverdue == 0) DueLightColor else OverdueLightColor,
          serviceForegroundColor =
            if (inputModel.servicesOverdue == 0) BlueTextColor else OverdueDarkRedColor,
          serviceMemberIcons = listOf(), // tODO
          serviceText = inputModel.members.count { it.pregnant == true }.toString()
        )
      is RegisterData.AncRegisterData ->
        RegisterViewData(
          id = inputModel.id,
          title = listOf(inputModel.name, inputModel.age).joinToString(),
          subtitle = inputModel.address,
          status = inputModel.visitStatus.name,
          otherStatus = "", // TODO
          serviceAsButton = true,
          serviceBackgroundColor =
            if (inputModel.servicesOverdue == 0) DueLightColor else OverdueLightColor,
          serviceForegroundColor =
            if (inputModel.servicesOverdue == 0) BlueTextColor else OverdueDarkRedColor,
          healthModule = HealthModule.ANC
        )
      else -> throw UnsupportedOperationException()
    }
  }
}
