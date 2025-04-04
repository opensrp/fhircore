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
  fun `removeHashPrefix should remove single hash prefix from string`() {
    val input = "#test123"
    val result = Utils.removeHashPrefix(input)
    assertEquals("test123", result)
  }

  @Test
  fun `removeHashPrefix should remove multiple hash prefixes from string`() {
    val input = "###test123"
    val result = Utils.removeHashPrefix(input)
    assertEquals("test123", result)
  }

  @Test
  fun `removeHashPrefix should return same string when no hash prefix is present`() {
    val input = "test123"
    val result = Utils.removeHashPrefix(input)
    assertEquals("test123", result)
  }

  @Test
  fun `removeHashPrefix should handle empty string`() {
    val input = ""
    val result = Utils.removeHashPrefix(input)
    assertEquals("", result)
  }

  @Test
  fun `removeHashPrefix should handle string with hash symbols in middle`() {
    val input = "test#123"
    val result = Utils.removeHashPrefix(input)
    assertEquals("test#123", result)
  }

  @Test
  fun `removeHashPrefix should handle integer with multiple hash prefixes`() {
    val input = "###123"
    val result = Utils.removeHashPrefix(input)
    assertEquals("123", result)
  }

  @Test
  fun `removeHashPrefix should handle integer without hash prefix`() {
    val input = 123
    val result = Utils.removeHashPrefix(input)
    assertEquals("123", result)
  }

  @Test
  fun `removeHashPrefix should handle other types by converting to string`() {
    val input = 123.45
    val result = Utils.removeHashPrefix(input)
    assertEquals("123.45", result)
  }

  @Test
  fun `removeHashPrefix should handle uuid with multiple hash prefixes`() {
    val input = "##8036ea0d-da4f-435c-bd4a-3e819a5a52dc"
    val result = Utils.removeHashPrefix(input)
    assertEquals("8036ea0d-da4f-435c-bd4a-3e819a5a52dc", result)
  }
}
