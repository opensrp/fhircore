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

package org.smartregister.fhircore.quest.ui.appointment.register

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.quest.ui.FilterOption

interface AppointmentFilterOption : FilterOption

enum class PatientAssignment : AppointmentFilterOption {
  ALL_PATIENTS,
  MY_PATIENTS;

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class PatientCategory : AppointmentFilterOption {
  ALL_PATIENT_CATEGORIES,
  ART_CLIENT,
  EXPOSED_INFANT,
  CHILD_CONTACT,
  PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY,
  SEXUAL_CONTACT;

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

enum class Reason(val patientCategory: Array<PatientCategory>) : AppointmentFilterOption {
  ALL_REASONS(PatientCategory.values()),
  CERVICAL_CANCER_SCREENING(arrayOf(PatientCategory.ART_CLIENT)),
  DBS_POSITIVE(arrayOf(PatientCategory.EXPOSED_INFANT)),
  HIV_TEST(
    arrayOf(
      PatientCategory.CHILD_CONTACT,
      PatientCategory.SEXUAL_CONTACT,
      PatientCategory.PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY
    )
  ),
  INDEX_CASE_TESTING(arrayOf(PatientCategory.ART_CLIENT)),
  MILESTONE_HIV_TEST(arrayOf(PatientCategory.EXPOSED_INFANT)),
  LINKAGE(arrayOf(PatientCategory.ART_CLIENT)),
  REFILL(arrayOf(PatientCategory.ART_CLIENT)),
  ROUTINE_VISIT(arrayOf(PatientCategory.EXPOSED_INFANT)),
  VIRAL_LOAD_COLLECTION(arrayOf(PatientCategory.ART_CLIENT)),
  WELCOME_SERVICE(arrayOf(PatientCategory.ART_CLIENT)),
  WELCOME_SERVICE_FOLLOW_UP(arrayOf(PatientCategory.ART_CLIENT));

  override fun text(): String =
    super.toString().lowercase().split("_").joinToString(" ") { it.capitalizeFirstLetter() }
}

class AppointmentDate(val value: Date) {
  fun formmatted(): CharSequence =
    SimpleDateFormat(FORMAT_STRING, Locale.getDefault()).format(value)

  companion object {
    private const val FORMAT_STRING = "dd - MM - yyyy"
  }
}

data class AppointmentFilter<T : FilterOption>(val selected: T, val options: Iterable<T>)
