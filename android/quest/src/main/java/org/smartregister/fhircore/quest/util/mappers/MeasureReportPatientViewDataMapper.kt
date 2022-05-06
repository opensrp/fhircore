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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.engine.util.extension.translateGender
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

class MeasureReportPatientViewDataMapper
@Inject
constructor(@ApplicationContext val context: Context) :
  DataMapper<RegisterData.AncRegisterData, MeasureReportPatientViewData> {
  override fun transformInputToOutputModel(
    inputModel: RegisterData.AncRegisterData
  ): MeasureReportPatientViewData {
    return MeasureReportPatientViewData(
      logicalId = inputModel.logicalId,
      name = inputModel.name,
      gender =
        Enumerations.AdministrativeGender.FEMALE.translateGender(context).first().uppercase(),
      age = inputModel.birthDate.toAgeDisplay(),
      family = context.getString(R.string.family_suffix, inputModel.familyName),
    )
  }
}
