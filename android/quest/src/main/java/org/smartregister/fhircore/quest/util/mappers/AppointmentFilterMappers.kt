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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.util.SystemConstants
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

fun transformAppointmentUiReasonToCode(uiReason: Reason): CodeableConcept? =
  when (uiReason) {
    Reason.CERVICAL_CANCER_SCREENING ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "via", "Cervical Cancer Screening"),
      )
    Reason.DBS_POSITIVE ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "DBS Pos", "DBS Positive"),
      )
    Reason.HIV_TEST ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "hiv-test", "HIV Test"),
      )
    Reason.INDEX_CASE_TESTING ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "ICT", "Index Case Testing"),
      )
    Reason.MILESTONE_HIV_TEST ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "Milestone", "Milestone HIV Test"),
      )
    Reason.LINKAGE ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "linkage", "Linkage"),
      )
    Reason.REFILL ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "Refill", "Refill"),
      )
    Reason.ROUTINE_VISIT ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "Routine", "Routine Visit"),
      )
    Reason.VIRAL_LOAD_COLLECTION ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "vl", "Viral Load Collection"),
      )
    Reason.WELCOME_SERVICE ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "ICT", "Index Case Testing"),
      )
    Reason.WELCOME_SERVICE_HVL ->
      CodeableConcept(
        Coding(SystemConstants.REASON_CODE_SYSTEM, "welcome-service-hvl", "Welcome Service HVL"),
      )
    Reason.WELCOME_SERVICE_FOLLOW_UP ->
      CodeableConcept(
        Coding(
          SystemConstants.REASON_CODE_SYSTEM,
          "welcome-service-follow-up",
          "Index Case Testing",
        ),
      )
    Reason.TB_HISTORY_REGIMEN ->
      CodeableConcept(
        Coding(
          SystemConstants.REASON_CODE_SYSTEM,
          "tb_history_and_regimen",
          "Welcome Service Follow Up",
        ),
      )
    else -> null
  }
