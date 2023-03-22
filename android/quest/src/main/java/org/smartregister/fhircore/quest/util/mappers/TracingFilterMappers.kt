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

import org.smartregister.fhircore.engine.data.local.TracingAgeFilterEnum
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.quest.ui.tracing.register.AgeFilter
import org.smartregister.fhircore.quest.ui.tracing.register.TracingPatientCategory
import org.smartregister.fhircore.quest.ui.tracing.register.TracingReason
import org.smartregister.fhircore.quest.ui.tracing.register.TracingRegisterFilterState

fun transformToTracingAgeFilterEnum(it: TracingRegisterFilterState) =
  when (it.age.selected) {
    AgeFilter.ALL_AGES -> null
    AgeFilter.`0_2_YEARS` -> TracingAgeFilterEnum.ZERO_TO_2
    AgeFilter.`0-18_YEARS` -> TracingAgeFilterEnum.ZERO_TO_18
    AgeFilter.`18_PLUS_YEARS` -> TracingAgeFilterEnum.`18_PLUS`
  }

fun transformPatientCategoryToHealthStatus(patientCategory: TracingPatientCategory) =
  when (patientCategory) {
    TracingPatientCategory.ALL_PATIENT_CATEGORIES -> null
    TracingPatientCategory.ART_CLIENT ->
      listOf(HealthStatus.CLIENT_ALREADY_ON_ART, HealthStatus.NEWLY_DIAGNOSED_CLIENT)
    TracingPatientCategory.EXPOSED_INFANT -> listOf(HealthStatus.EXPOSED_INFANT)
    TracingPatientCategory.CHILD_CONTACT -> listOf(HealthStatus.CHILD_CONTACT)
    TracingPatientCategory.PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY ->
      listOf(HealthStatus.COMMUNITY_POSITIVE)
    TracingPatientCategory.SEXUAL_CONTACT -> listOf(HealthStatus.SEXUAL_CONTACT)
  }

fun transformTracingUiReasonToCode(uiReason: TracingReason) =
  when (uiReason) {
    TracingReason.ALL_REASONS -> null
    TracingReason.LINKAGE -> "linkage"
    TracingReason.HIGH_VIRAL_LOAD -> "hvl"
    TracingReason.DETECTABLE_VIRAL_LOAD -> "detectable-vl"
    TracingReason.MISSING_VIRAL_LOAD -> "missing-vl"
    TracingReason.INVALID_VIRAL_LOAD -> "invalid-vl"
    TracingReason.INTERRUPTED_TREATMENT -> "interrupt-treat"
    TracingReason.MISSED_APPOINTMENT -> "miss-appt"
    TracingReason.DUAL_REFERRAL -> "dual-referral"
    TracingReason.CONTRACT_REFERRAL -> "contact-referral"
    TracingReason.PROVIDER_REFERRAL -> "provider-referral"
    TracingReason.MISSED_CLINIC_APPOINTMENT -> "miss-clinic-appt"
    TracingReason.FAMILY_REFERRAL_SERVICES_SELF_TEST_KIT -> "frs-stk"
    TracingReason.FAMILY_REFERRAL_SERVICES_NO_SELF_TEST_KIT -> "frs-no-stk"
    TracingReason.DRY_BLOOD_SAMPLE_POSITIVE_RESULT -> "dbs-positive"
    TracingReason.DRY_BLOOD_SAMPLE_RESULT -> "dbs-missing"
    TracingReason.DRY_BLOOD_SAMPLE_INVALID -> "dbs-invalid"
    TracingReason.MISSED_MILESTONE_VISIT -> "miss-milestone"
    TracingReason.MISSED_ROUTINE_VISIT -> "miss-routine"
  }
