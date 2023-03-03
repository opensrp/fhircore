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

package org.smartregister.fhircore.quest.util

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class IterableUtilKtTest {
  private lateinit var map: Map<String, Int?>

  @Before
  fun setUp() {
    map = mapOf("Vanilla" to 24, "Chocolate" to 14, "Rocky Road" to null)
  }

  @Test
  fun testShouldReturnDefaultValueIfKeyNull() {
    val defaultValue = 1
    Assert.assertEquals(defaultValue, nonNullGetOrDefault(map, null, defaultValue))
  }

  @Test
  fun testShouldReturnDefaultValueIfNoKey() {
    val defaultValue = 1
    Assert.assertEquals(defaultValue, nonNullGetOrDefault(map, "A", defaultValue))
  }

  @Test
  fun testShouldReturnNullIfValueNull() {
    val defaultValue = 1
    Assert.assertEquals(null, nonNullGetOrDefault(map, "Rocky Road", defaultValue))
  }

  @Test
  fun testShouldReturnValueIfKeyExists() {
    val defaultValue = 1
    Assert.assertEquals(24, nonNullGetOrDefault(map, "Vanilla", defaultValue))
  }
}
