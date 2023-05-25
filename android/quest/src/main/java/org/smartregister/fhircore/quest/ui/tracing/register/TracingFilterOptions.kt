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

package org.smartregister.fhircore.quest.ui.tracing.register

import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.quest.ui.FilterOption

interface TracingFilterOption : FilterOption

interface TracingPatientAssignment : TracingFilterOption {
  fun assignedToMe(): Boolean
}

enum class PhonePatientAssignment : TracingPatientAssignment {
  ALL_PHONE,
  ASSIGNED_TO_ME {
    override fun assignedToMe(): Boolean = true
  };

  override fun assignedToMe(): Boolean = false

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class HomePatientAssignment : TracingPatientAssignment {
  ALL_HOME,
  ASSIGNED_TO_ME {
    override fun assignedToMe(): Boolean = true
  };

  override fun assignedToMe(): Boolean = false

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class TracingPatientCategory : TracingFilterOption {
  ALL_PATIENT_CATEGORIES,
  ART_CLIENT,
  EXPOSED_INFANT,
  CHILD_CONTACT,
  PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY,
  SEXUAL_CONTACT;

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class TracingReason : TracingFilterOption {
  ALL_REASONS,
  LINKAGE,
  HIGH_VIRAL_LOAD,
  DETECTABLE_VIRAL_LOAD,
  MISSING_VIRAL_LOAD,
  INVALID_VIRAL_LOAD,
  INTERRUPTED_TREATMENT,
  MISSED_APPOINTMENT,
  DUAL_REFERRAL,
  CONTRACT_REFERRAL,
  PROVIDER_REFERRAL,
  MISSED_CLINIC_APPOINTMENT,
  FAMILY_REFERRAL_SERVICES_SELF_TEST_KIT,
  FAMILY_REFERRAL_SERVICES_NO_SELF_TEST_KIT,
  DRY_BLOOD_SAMPLE_POSITIVE_RESULT,
  DRY_BLOOD_SAMPLE_RESULT,
  DRY_BLOOD_SAMPLE_INVALID,
  MISSED_MILESTONE_VISIT,
  MISSED_ROUTINE_VISIT;

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class AgeFilter : TracingFilterOption {
  ALL_AGES,
  `0_2_YEARS` {
    override fun text() = "0 -2 Years"
  },
  `0-18_YEARS` {
    override fun text() = "0 - 18 Years"
  },
  `18_PLUS_YEARS` {
    override fun text() = "18+ Years"
  };

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

data class TracingRegisterUiFilter<T : TracingFilterOption>(
  val selected: T,
  val options: Iterable<T>
)
