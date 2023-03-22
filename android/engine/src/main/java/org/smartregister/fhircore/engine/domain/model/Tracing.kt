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

package org.smartregister.fhircore.engine.domain.model

import java.util.Date

data class TracingHistory(
  val historyId: String,
  val startDate: Date,
  val endDate: Date?,
  val isActive: Boolean,
  val numberOfAttempts: Int
)

data class TracingOutcome(
  val historyId: String,
  val encounterId: String,
  val title: String,
  val date: Date?,
)

data class TracingOutcomeDetails(
  val title: String,
  val date: Date,
  val dateOfAppointment: Date?,
  val reasons: List<String>,
  val outcome: String,
  val conducted: Boolean,
)

data class TracingAttempt(
  val historyId: String?,
  val lastAttempt: Date?,
  val numberOfAttempts: Int,
  val outcome: String,
  val reasons: List<String>,
)
