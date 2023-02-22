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

import org.junit.Assert
import org.junit.Test

class MeasureReportConfigurationTest {

  private val reportsList =
    listOf(
      MeasureReportConfig(
        id = "1234",
        title = "title",
        description = "description",
        url = "measureURL",
        module = "module"
      )
    )

  private val measureReportConfiguration =
    MeasureReportConfiguration(
      appId = "1234",
      configType = "configType",
      id = "2424",
      registerId = "12345",
      registerDate = "registerDate",
      showFixedRangeSelection = false,
      reports = reportsList
    )

  @Test
  fun testMeasureReportConfiguration() {
    Assert.assertEquals("1234", measureReportConfiguration.appId)
    Assert.assertEquals("configType", measureReportConfiguration.configType)
    Assert.assertEquals("2424", measureReportConfiguration.id)
    Assert.assertEquals("12345", measureReportConfiguration.registerId)
    Assert.assertEquals("registerDate", measureReportConfiguration.registerDate)
    Assert.assertEquals(false, measureReportConfiguration.showFixedRangeSelection)
    Assert.assertEquals(reportsList, measureReportConfiguration.reports)
  }
}
