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

package org.smartregister.fhircore.engine.util.extension

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.RoundingStrategy
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class BigDecimalExtensionTest : RobolectricTest() {

  @Test
  fun testRoundingShouldRoundAndReturnCorrectDecimalPrecision() {
    val number = BigDecimal("20.76425436")

    val resultTruncate = number.rounding(RoundingStrategy.TRUNCATE, 0)
    val resultRoundUp = number.rounding(RoundingStrategy.ROUND_UP, 0)
    val resultRoundOffLower = BigDecimal("20.3333331").rounding(RoundingStrategy.ROUND_OFF, 0)
    val resultRoundOffUpper = number.rounding(RoundingStrategy.ROUND_OFF, 0)
    val resultPrecision2Truncate = number.rounding(RoundingStrategy.TRUNCATE, 2)
    val resultPrecision2RoundUp = number.rounding(RoundingStrategy.ROUND_UP, 2)
    val resultPrecision2RoundOff = number.rounding(RoundingStrategy.ROUND_OFF, 2)

    Assert.assertEquals("20", resultTruncate)
    Assert.assertEquals("21", resultRoundUp)
    Assert.assertEquals("20", resultRoundOffLower)
    Assert.assertEquals("21", resultRoundOffUpper)
    Assert.assertEquals("20.76", resultPrecision2Truncate)
    Assert.assertEquals("20.77", resultPrecision2RoundUp)
    Assert.assertEquals("20.76", resultPrecision2RoundOff)
  }
}
