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

package org.smartregister.fhircore.engine.util

import org.junit.Assert
import org.junit.Test

class LocaleUtilTest {

  @Test
  fun testGenerateIdentifierReturnsCorrectKey() {

    val result = LocaleUtil.generateIdentifier("OVERDUE")
    Assert.assertEquals("overdue", result)
  }

  @Test
  fun testGenerateIdentifierWithDigitPrefixParamReturnsCorrectKey() {
    val result = LocaleUtil.generateIdentifier("40 Weeks")
    Assert.assertEquals("_40_weeks", result)
  }

  @Test
  fun testGenerateIdentifierWithWhitespacesParamReturnsCorrectKey() {

    val result = LocaleUtil.generateIdentifier("Home Address")
    Assert.assertEquals("home_address", result)
  }
}
