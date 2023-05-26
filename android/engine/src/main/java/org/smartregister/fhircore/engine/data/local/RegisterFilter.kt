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

package org.smartregister.fhircore.engine.data.local

import java.util.Date
import org.smartregister.fhircore.engine.domain.model.HealthStatus

sealed interface RegisterFilter

data class AppointmentRegisterFilter(
  val dateOfAppointment: Date,
  val myPatients: Boolean,
  val patientCategory:
    Iterable<
      HealthStatus>?, // nullable whereby null represents absence of filter by patient category
  val reasonCode: String? // nullable whereby null represents absence of filter by reason code
) : RegisterFilter

enum class TracingAgeFilterEnum {
  ZERO_TO_2,
  ZERO_TO_18,
  `18_PLUS`
}

data class TracingRegisterFilter(
  val isAssignedToMe: Boolean,
  val patientCategory:
    Iterable<
      HealthStatus>?, // nullable whereby null represents absence of filter by patient category
  val reasonCode: String?, // nullable whereby null represents absence of filter by reason code
  val age: TracingAgeFilterEnum? // nullable whereby null represents absence of filter by age
) : RegisterFilter
