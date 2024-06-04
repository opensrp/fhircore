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

package org.smartregister.fhircore.quest.ui.patient.profile.tranfer

import com.google.android.fhir.datacapture.extensions.logicalId
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.SystemConstants
import org.smartregister.fhircore.engine.util.extension.extractGivenName
import org.smartregister.fhircore.engine.util.extension.extractName

data class TransferOutScreenState(
  val patientId: String,
  val facilityId: String,
  val firstName: String,
  val fullName: String,
) {
  companion object {
    fun fromPatient(patient: Patient): TransferOutScreenState {
      return TransferOutScreenState(
        patientId = patient.logicalId,
        facilityId =
          patient.meta.tag.firstOrNull { it.system == SystemConstants.LOCATION_TAG }?.code ?: "NA",
        firstName = patient.extractGivenName(),
        fullName = patient.extractName(),
      )
    }
  }
}
