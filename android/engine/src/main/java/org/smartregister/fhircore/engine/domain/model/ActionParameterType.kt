/*
 * Copyright 2021-2023 Ona Systems, Inc
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

@file:OptIn(ExperimentalSerializationApi::class)

package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

/** Represents different types of parameters that can be defined within the config actions */
@Suppress("EXPLICIT_SERIALIZABLE_IS_REQUIRED")
enum class ActionParameterType {
  /** Represents parameters that are used to pre-populate Questionnaire items with initial values */
  @JsonNames("pre_populate", "PrePopulate") PREPOPULATE,
  @JsonNames("param_data", "ParamData") PARAMDATA,
  @JsonNames("update_date_on_edit", "UpdateDateOnEdit") UPDATE_DATE_ON_EDIT
}
