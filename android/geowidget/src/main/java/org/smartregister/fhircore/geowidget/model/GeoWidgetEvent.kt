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

package org.smartregister.fhircore.geowidget.model

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Serializable
sealed class GeoWidgetEvent {

  @ExcludeFromJacocoGeneratedReport
  @Serializable
  data class OpenProfile(val data: String, val geoWidgetConfiguration: GeoWidgetConfiguration) :
    GeoWidgetEvent()

  @ExcludeFromJacocoGeneratedReport
  @Serializable
  data class RegisterClient(val data: String, val questionnaire: QuestionnaireConfig) :
    GeoWidgetEvent()
}
