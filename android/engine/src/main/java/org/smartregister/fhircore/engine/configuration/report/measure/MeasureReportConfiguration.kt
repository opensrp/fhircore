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

package org.smartregister.fhircore.engine.configuration.report.measure

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration

@Serializable
data class MeasureReportConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.MeasureReport.name,
  val id: String,
  val registerId: String,
  val startPeriod: String? = DEFAULT_START_PERIOD,
  val showFixedRangeSelection: Boolean? = null,
  val showSubjectSelection: Boolean? = null,
  val scheduledGenerationDuration: String? = null,
  val reports: List<ReportConfiguration> = emptyList(),
) : Configuration()

/**
 * The list of months will show as far back as this date. This cannot be null. Consider setting
 * through configuration if this must be varied.
 */
const val DEFAULT_START_PERIOD = "2023-01-01"
