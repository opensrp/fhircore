/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration.report.indicator

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.domain.model.ResourceConfig

@Serializable
data class ReportIndicatorConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.ReportIndicator.name,
  val id: String,
  val indicators: List<ReportIndicator> = emptyList(),
) : Configuration()

@Serializable
data class ReportIndicator(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val resourceConfig: ResourceConfig,
)
