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

import org.junit.Assert
import org.junit.Test

class StringExtensionTest {

  @Test
  fun practitionerEndpointUrlShouldMatch() {
    Assert.assertEquals(
      "practitioner-details?keycloak-uuid=my-keycloak-id",
      "my-keycloak-id".practitionerEndpointUrl()
    )
  }

  @Test
  fun shouldRemoveExtraSpaces() {
    val beforeFormatExampleOne = "Aurang    zaib        umer   ,   M,           43y"
    val beforeFormatExampleTwo = "  Aurang    zaib   umer   , M, 43y          "
    val expected = "Aurang zaib umer, M, 43y"

    Assert.assertEquals(expected, beforeFormatExampleOne.removeExtraWhiteSpaces())
    Assert.assertEquals(expected, beforeFormatExampleTwo.removeExtraWhiteSpaces())
  }
}
