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

package org.smartregister.fhircore.sdk

import ca.uhn.fhir.model.api.annotation.SearchParamDefinition
import ca.uhn.fhir.rest.gclient.StringClientParam
import org.hl7.fhir.r4.model.Patient

class PatientExtended : Patient() {
  @SearchParamDefinition(
    name = "tag",
    path = "Patient.meta.tag.display",
    description = "Tag",
    type = "string"
  )
  @JvmField
  val SP_TAG = TAG_KEY

  @SearchParamDefinition(
    name = "profile",
    path = "Patient.meta.profile",
    description = "Profile",
    type = "string"
  )
  @JvmField
  val SP_PROFILE = PROFILE_KEY

  companion object {
    const val TAG_KEY = "tag"
    const val PROFILE_KEY = "profile"

    val TAG = StringClientParam(TAG_KEY)
    val PROFILE = StringClientParam(PROFILE_KEY)
  }
}
