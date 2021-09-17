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

package org.smartregister.fhircore.anc.sdk

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.annotation.SearchParamDefinition
import ca.uhn.fhir.rest.gclient.StringClientParam
import org.hl7.fhir.r4.model.Patient

// TODO remove and handle usages when SDK supports tags and profile as searchable
class PatientExtended : Patient() {
  @SearchParamDefinition(
    name = TAG_KEY,
    path = "Patient.meta.tag.display",
    description = "Tag",
    type = "string"
  )
  @JvmField
  val SP_TAG = TAG_KEY

  companion object {
    const val TAG_KEY = "_tag"
    val TAG = StringClientParam(TAG_KEY)
  }
}

fun Patient.extractExtendedPatient(): PatientExtended {
  val parser = FhirContext.forR4().newJsonParser()
  val patientEncoded = parser.encodeResourceToString(this)
  return parser.parseResource(PatientExtended::class.java, patientEncoded)
}
