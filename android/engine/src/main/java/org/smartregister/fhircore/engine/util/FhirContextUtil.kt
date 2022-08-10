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

package org.smartregister.fhircore.engine.util

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.smartregister.model.location.LocationHierarchy
import org.smartregister.model.practitioner.FhirPractitionerDetails
import org.smartregister.model.practitioner.KeycloakUserDetails
import org.smartregister.model.practitioner.PractitionerDetails
import org.smartregister.model.practitioner.UserBioData

object FhirContextUtil {

  fun getPractitionerDetailParser(): IParser {
    val fhirContext = FhirContext.forR4Cached()

    fhirContext.registerCustomType(PractitionerDetails::class.java)
    fhirContext.registerCustomType(FhirPractitionerDetails::class.java)
    fhirContext.registerCustomType(LocationHierarchy::class.java)
    fhirContext.registerCustomType(KeycloakUserDetails::class.java)
    fhirContext.registerCustomType(UserBioData::class.java)

    return fhirContext.newJsonParser()
  }
}
