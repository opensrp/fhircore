/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.util.DataMapper
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

class MeasureReportPatientViewDataMapper
@Inject
constructor(
  @ApplicationContext val context: Context,
  val fhirPathDataExtractor: FhirPathDataExtractor
) : DataMapper<ResourceData, MeasureReportPatientViewData> {

  override fun transformInputToOutputModel(inputModel: ResourceData): MeasureReportPatientViewData {
    // TODO Refactor measure reporting register to use register configuration
    //  and update MeasureReportPatientViewDataMapperTest#testMapToOutputModelPatient()
    //  once refactor is complete
    /*
    // Patient resource can be the baseResource or any of the relatedResources of the resourceData
    // Ensure the register is configured to return a Patient resource; app will crash otherwise
    val patient: Patient =
      when (inputModel.baseResource) {
        is Patient -> inputModel.baseResource as Patient
        else ->
          inputModel.relatedResourcesMap.getValue(ResourceType.Patient.name).first() as Patient
      }*/

    return MeasureReportPatientViewData(
      logicalId = inputModel.baseResourceId,
      name = "",
      gender = "",
      age = ",",
      family = ""
    )
  }
}
