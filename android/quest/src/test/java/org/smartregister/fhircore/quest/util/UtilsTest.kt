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

package org.smartregister.fhircore.quest.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [Utils] class. */
class UtilsTest {

  @Test
  fun `removeHashPrefix should remove hash prefix when present`() {
    // Given
    val input = "#test123"

    // When
    val result = Utils.removeHashPrefix(input)

    // Then
    assertEquals("test123", result)
  }

  @Test
  fun `removeHashPrefix should return same string when hash prefix is not present`() {
    // Given
    val input = "test123"

    // When
    val result = Utils.removeHashPrefix(input)

    // Then
    assertEquals("test123", result)
  }

  @Test
  fun `removeHashPrefix should handle empty string`() {
    // Given
    val input = ""

    // When
    val result = Utils.removeHashPrefix(input)

    // Then
    assertEquals("", result)
  }

  @Test
  fun `removeHashPrefix should handle string with multiple hash symbols`() {
    // Given
    val input = "#test#123"

    // When
    val result = Utils.removeHashPrefix(input)

    // Then
    assertEquals("test#123", result)
  }
}
