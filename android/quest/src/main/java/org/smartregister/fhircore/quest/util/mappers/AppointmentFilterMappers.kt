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

import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.quest.ui.appointment.register.PatientCategory
import org.smartregister.fhircore.quest.ui.appointment.register.Reason

fun transformPatientCategoryToHealthStatus(patientCategory: PatientCategory) =
  when (patientCategory) {
    PatientCategory.ALL_PATIENT_CATEGORIES -> null
    PatientCategory.ART_CLIENT ->
      listOf(HealthStatus.CLIENT_ALREADY_ON_ART, HealthStatus.NEWLY_DIAGNOSED_CLIENT)
    PatientCategory.EXPOSED_INFANT -> listOf(HealthStatus.EXPOSED_INFANT)
    PatientCategory.CHILD_CONTACT -> listOf(HealthStatus.CHILD_CONTACT)
    PatientCategory.PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY ->
      listOf(HealthStatus.COMMUNITY_POSITIVE)
    PatientCategory.SEXUAL_CONTACT -> listOf(HealthStatus.SEXUAL_CONTACT)
  }

fun transformAppointmentUiReasonToCode(uiReason: Reason) =
  when (uiReason) {
    Reason.CERVICAL_CANCER_SCREENING -> "VIA"
    Reason.DBS_POSITIVE -> "DBS Pos"
    Reason.HIV_TEST -> "HIV Test"
    Reason.INDEX_CASE_TESTING -> "ICT"
    Reason.MILESTONE_HIV_TEST -> "Milestone"
    Reason.LINKAGE -> "Linkage"
    Reason.REFILL -> "Refill"
    Reason.ROUTINE_VISIT -> "Routine"
    Reason.VIRAL_LOAD_COLLECTION -> "VL"
    Reason.WELCOME_SERVICE -> "Welcome"
    Reason.WELCOME_SERVICE_FOLLOW_UP -> "Welcome Service Follow Up"
    else -> null
  }
