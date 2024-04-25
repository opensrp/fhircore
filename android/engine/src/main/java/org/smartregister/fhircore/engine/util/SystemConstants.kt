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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding

object SystemConstants {
  const val reasonCodeSystem = "https://d-tree.org/fhir/reason-code"
  const val baseUrl = "https://d-tree.org"
  const val taskFilterTagSystem = "https://d-tree.org/fhir/task-filter-tag"
  const val taskTaskOrderSystem = "https://d-tree.org/fhir/clinic-visit-task-order"
  const val patientTypeFilterTagViaMetaCodingSystem = "https://d-tree.org/fhir/patient-meta-tag"
  const val contactTracingSystem = "https://d-tree.org/fhir/contact-tracing"
  const val observationCodeSystem = "https://d-tree.org/fhir/observation-codes"
  const val carePlanReferenceSystem = "https://d-tree.org/fhir/careplan-reference"
}

object ReasonConstants {
  val WelcomeServiceCode =
    CodeableConcept(Coding(SystemConstants.reasonCodeSystem, "Welcome", "Welcome Service")).apply {
      text = "Welcome Service"
    }

  const val tracingOutComeCode = "tracing-outcome"
  const val dateOfAgreedAppointmnet = "date-of-agreed-appointment"
}
