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

package org.smartregister.fhircore.engine.task

import com.google.android.fhir.FhirEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

class FhirTaskGenerator(val patient: Patient) {

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  private val simpleDataFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

  private val placeholdersValuesMap: MutableMap<String, String> = mutableMapOf()

  init {
    initializePlaceholders()
  }

  private fun initializePlaceholders() {
    placeHolderValues("patient_id", "Patient.id", true)
    placeHolderValues("auto_generated_uuid", UUID.randomUUID().toString(), true)
    val today = Date()
    placeHolderValues("date_patient_is_registered", simpleDataFormatter.format(today), true)
    val patientBirthDate: Date? =
      simpleDataFormatter.parse(
        fhirPathDataExtractor.extractData(patient, "Patient.birthDate").first().primitiveValue()
      )
    if (patientBirthDate != null) {
      placeHolderValues("date_patient_is_older_than_five", simpleDataFormatter.format(today), true)
    }
  }

  fun generateTasks(baseCarePlan: String, baseTask: String) {}

  private fun placeHolderValues(placeHolder: String, value: String, isExpression: Boolean = false) {
    placeholdersValuesMap[placeHolder] =
      if (isExpression) fhirPathDataExtractor.extractData(patient, value).first().primitiveValue()
      else value
  }
  companion object {
    private const val DATE_FORMAT = "yyyy-MM-dd"
  }
}
