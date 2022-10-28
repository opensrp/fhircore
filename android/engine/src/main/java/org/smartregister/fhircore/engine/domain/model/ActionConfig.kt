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

import android.os.Bundle
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.util.extension.interpolate

@Serializable
data class ActionConfig(
  val trigger: ActionTrigger,
  val workflow: ApplicationWorkflow? = null,
  val id: String? = null,
  val rules: List<RuleConfig>? = null,
  val questionnaire: QuestionnaireConfig? = null,
  val params: List<ActionParameter> = emptyList(),
  val resourceConfig: FhirResourceConfig? = null
) {
  fun paramsBundle(computedValuesMap: Map<String, Any> = emptyMap()): Bundle =
    Bundle().apply {
      params.map { Pair(it.key, it.value.interpolate(computedValuesMap)) }.forEach {
        putString(it.first, it.second)
      }
    }
}

@Serializable
data class ActionParameter(
  val key: String,
  val paramType: ActionParameterType? = null,
  val dataType: DataType? = null,
  val value: String,
  val linkId: String? = null
) : java.io.Serializable
