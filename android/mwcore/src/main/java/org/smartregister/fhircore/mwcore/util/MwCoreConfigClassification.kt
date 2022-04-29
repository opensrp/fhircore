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

package org.smartregister.fhircore.mwcore.util

import org.smartregister.fhircore.engine.configuration.ConfigClassification

enum class MwCoreConfigClassification : ConfigClassification {
  PATIENT_REGISTER,
  PATIENT_REGISTER_ROW,
  TEST_RESULT_DETAIL_VIEW,
  PATIENT_DETAILS_VIEW,
  CONTROL_TEST_DETAILS_VIEW,
  RESULT_DETAILS_NAVIGATION,
  PATIENT_TASK_REGISTER;
  override val classification: String = name.lowercase()
}
