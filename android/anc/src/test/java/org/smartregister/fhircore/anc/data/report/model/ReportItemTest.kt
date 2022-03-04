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

package org.smartregister.fhircore.anc.data.report.model

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class ReportItemTest : RobolectricTest() {

  @Test
  fun testVerifyReportItemProperties() {

    with(ReportItem()) {
      Assert.assertEquals("", id)
      Assert.assertEquals("", title)
      Assert.assertEquals("", description)
      Assert.assertEquals("", reportType)
      Assert.assertEquals("", name)
    }

    with(ReportItem("1", "Title", "Description", "Report Type", "name")) {
      Assert.assertEquals("1", id)
      Assert.assertEquals("Title", title)
      Assert.assertEquals("Description", description)
      Assert.assertEquals("Report Type", reportType)
      Assert.assertEquals("name", name)
    }
  }
}
