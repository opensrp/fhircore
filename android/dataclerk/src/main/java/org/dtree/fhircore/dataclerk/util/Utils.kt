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

package org.dtree.fhircore.dataclerk.util

import android.content.res.Resources
import java.time.LocalDate
import java.time.Period
import org.dtree.fhircore.dataclerk.R
import org.dtree.fhircore.dataclerk.ui.main.PatientItem
import org.hl7.fhir.r4.model.Practitioner
import org.smartregister.fhircore.engine.util.extension.canonicalName

fun Practitioner.extractName(): String {
  if (!hasName()) return ""
  return this.name.canonicalName()
}

fun getFormattedAge(patientItem: PatientItem, resources: Resources): String {
  if (patientItem.dob == null) return ""
  return Period.between(patientItem.dob, LocalDate.now()).let {
    when {
      it.years > 0 -> resources.getQuantityString(R.plurals.ageYear, it.years, it.years)
      it.months > 0 -> resources.getQuantityString(R.plurals.ageMonth, it.months, it.months)
      else -> resources.getQuantityString(R.plurals.ageDay, it.days, it.days)
    }
  }
}
