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

package org.dtree.fhircore.dataclerk.ui.patient

import org.dtree.fhircore.dataclerk.ui.main.PatientItem

sealed class PatientDetailScreenState {
  object Loading : PatientDetailScreenState()
  data class Success(val patientDetail: PatientItem, val detailsData: List<PatientDetailData>) :
    PatientDetailScreenState()
  data class Error(val message: String) : PatientDetailScreenState()
}

interface PatientDetailData {
  val firstInGroup: Boolean
  val lastInGroup: Boolean
}

data class PatientDetailHeader(
  val header: String,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailProperty(
  val patientProperty: PatientProperty,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailOverview(
  val patient: PatientItem,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientProperty(val header: String, val value: String)
