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
import org.smartregister.fhircore.engine.configuration.view.SearchFilter

@Stable
@Serializable
data class DataDetailsListViewConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val contentTitle: String = "Content Title",
  val valuePrefix: String = "Value Prefix",
  val dataRowClickable: Boolean = true,
  val dynamicRows: List<List<Filter>> = listOf(),
  val questionnaireFilter: SearchFilter? = null,
  val questionnaireFieldsFilter: List<QuestionnaireItemFilter> = listOf()
) : Configuration

@Stable
fun dataDetailsListViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_details",
  contentTitle: String = "Content Title",
  valuePrefix: String = "Value Prefix",
  dataRowClickable: Boolean = true,
  dynamicRows: List<List<Filter>> = mutableListOf(),
  questionnaireFilter: SearchFilter? = null,
  questionnaireFieldsFilter: List<QuestionnaireItemFilter> = listOf()
) =
  DataDetailsListViewConfiguration(
    appId = appId,
    classification = classification,
    contentTitle = contentTitle,
    valuePrefix = valuePrefix,
    dataRowClickable = dataRowClickable,
    dynamicRows = dynamicRows,
    questionnaireFilter = questionnaireFilter,
    questionnaireFieldsFilter = questionnaireFieldsFilter
  )
