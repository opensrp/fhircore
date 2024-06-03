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
  const val REASON_CODE_SYSTEM = "https://d-tree.org/fhir/reason-code"
  const val BASE_URL = "https://d-tree.org"
  const val TASK_FILTER_TAG_SYSTEM = "https://d-tree.org/fhir/task-filter-tag"
  const val RESOURCE_CREATED_ON_TAG_SYSTEM = "https://d-tree.org/fhir/created-on-tag"
  const val TASK_TASK_ORDER_SYSTEM = "https://d-tree.org/fhir/clinic-visit-task-order"
  const val PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM =
    "https://d-tree.org/fhir/patient-meta-tag"
  const val CONTACT_TRACING_SYSTEM = "https://d-tree.org/fhir/contact-tracing"
  const val OBSERVATION_CODE_SYSTEM = "https://d-tree.org/fhir/observation-codes"
  const val CARE_PLAN_REFERENCE_SYSTEM = "https://d-tree.org/fhir/careplan-reference"
  const val QUESTIONNAIRE_REFERENCE_SYSTEM = "https://d-tree.org/fhir/procedure-code"
  const val LOCATION_TAG = "http://smartregister.org/fhir/location-tag"
  const val LOCATION_HIERARCHY_BINARY = "location-hierarchy"
}

object ReasonConstants {
  // TODO: change code  to "welcome-service"
  val WelcomeServiceCode =
    CodeableConcept(Coding(SystemConstants.REASON_CODE_SYSTEM, "Welcome", "Welcome Service"))
      .apply { text = "Welcome Service" }

  val homeTracingCoding =
    Coding(SystemConstants.CONTACT_TRACING_SYSTEM, "home-tracing", "Home Tracing")
  val phoneTracingCoding =
    Coding(SystemConstants.CONTACT_TRACING_SYSTEM, "phone-tracing", "Phone Tracing")

  var missedAppointmentTracingCode =
    Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-appointment", "Missed Appointment")
  var missedMilestoneAppointmentTracingCode =
    Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-milestone", "Missed Milestone Appointment")
  var missedRoutineAppointmentTracingCode =
    Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-routine", "Missed Routine Appointment")
  var interruptedTreatmentTracingCode =
    Coding(SystemConstants.REASON_CODE_SYSTEM, "interrupted-treatment", "Interrupted Treatment")

  var pendingTransferOutCode =
    Coding("https://d-tree.org/fhir/transfer-out-status", "pending", "Pending")

  const val TRACING_OUTCOME_CODE = "tracing-outcome"
  const val DATE_OF_AGREED_APPOINTMENT = "date-of-agreed-appointment"
}
