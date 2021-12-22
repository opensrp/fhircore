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

package org.smartregister.fhircore.quest.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
class PatientRegisterRowViewConfiguration(
  override val appId: String,
  override val classification: String,
  var showG6pdStatus: Boolean = false
) : Configuration

/**
 * @param appId Set unique identifier for this configuration
 * @param classification set the classification
 * @param showG6pdStatus enable of disable the status
 */
@Stable
fun patientRegisterRowViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_list_row",
  showG6pdStatus: Boolean = false
) =
  PatientRegisterRowViewConfiguration(
    appId = appId,
    classification = classification,
    showG6pdStatus = showG6pdStatus
  )
