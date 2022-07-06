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

package org.smartregister.fhircore.engine.configuration.view

import org.junit.Assert
import org.junit.Test

class MeasureReportConfigurationTest {

  @Test
  fun testMeasureReportConfiguration() {
    val configuration =
      MeasureReportConfiguration(
        appId = "anc",
        classification = "classification",
        reports =
          listOf(
            MeasureReportRowConfig(
              id = "my-id",
              name = "my-name",
              reportType = "population",
              description = "my description",
              title = "my title"
            )
          )
      )
    Assert.assertEquals("anc", configuration.appId)
    Assert.assertEquals("classification", configuration.classification)
    Assert.assertEquals("my-id", configuration.reports.first().id)
    Assert.assertEquals("my-name", configuration.reports.first().name)
    Assert.assertEquals("population", configuration.reports.first().reportType)
    Assert.assertEquals("my title", configuration.reports.first().title)
    Assert.assertEquals("my description", configuration.reports.first().description)
  }

  @Test
  fun testMeasureReportConfigurationOf() {
    val configuration =
      measureReportConfigurationOf(
        appId = "anc",
        classification = "classification",
        reports =
          listOf(
            MeasureReportRowConfig(
              id = "my-id",
              name = "my-name",
              reportType = "population",
              description = "my description",
              title = "my title"
            )
          )
      )
    Assert.assertEquals("anc", configuration.appId)
    Assert.assertEquals("classification", configuration.classification)
    Assert.assertEquals("my-id", configuration.reports.first().id)
    Assert.assertEquals("my-name", configuration.reports.first().name)
    Assert.assertEquals("population", configuration.reports.first().reportType)
    Assert.assertEquals("my title", configuration.reports.first().title)
    Assert.assertEquals("my description", configuration.reports.first().description)
  }

  @Test
  fun testMeasureReportConfigurationOf_default() {
    val configuration = measureReportConfigurationOf()
    Assert.assertEquals("", configuration.appId)
    Assert.assertEquals("report", configuration.classification)
    Assert.assertEquals(0, configuration.reports.size)
  }
}
