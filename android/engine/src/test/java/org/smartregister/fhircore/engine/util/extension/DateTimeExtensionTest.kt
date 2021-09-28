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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.DateType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class DateTimeExtensionTest : RobolectricTest() {

  @Test
  fun testPlusWeeksAsStringShouldAddWeeksAndReturnFormattedDate() {
    val date = DateType("2012-10-12")

    val formatted = date.plusWeeksAsString(2)

    assertTrue("2012-10-25".contentEquals(formatted))
  }

  @Test
  fun testPlusWeeksAsStringShouldAddMonthsAndReturnFormattedDate() {
    val date = DateType("2012-10-12")

    val formatted = date.plusMonthsAsString(3)

    assertTrue("2013-01-11".contentEquals(formatted))
  }
}
