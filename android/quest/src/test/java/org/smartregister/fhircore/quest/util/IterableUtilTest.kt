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
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType

class IterableUtilTest {
  private lateinit var map: Map<String, Int?>

  @Before
  fun setUp() {
    map = mapOf("Vanilla" to 24, "Chocolate" to 14, "Rocky Road" to null)
  }

  @Test
  fun testNonNullGetOrDefaultShouldReturnDefaultValueIfKeyNull() {
    val defaultValue = 1
    Assert.assertEquals(defaultValue, nonNullGetOrDefault(map, null, defaultValue))
  }

  @Test
  fun testNonNullGetOrDefaultShouldReturnDefaultValueIfNoKey() {
    val defaultValue = 1
    Assert.assertEquals(defaultValue, nonNullGetOrDefault(map, "A", defaultValue))
  }

  @Test
  fun testNonNullGetOrDefaultShouldReturnNullIfValueNull() {
    val defaultValue = 1
    Assert.assertEquals(null, nonNullGetOrDefault(map, "Rocky Road", defaultValue))
  }

  @Test
  fun testNonNullGetOrDefaultShouldReturnValueIfKeyExists() {
    val defaultValue = 1
    Assert.assertEquals(24, nonNullGetOrDefault(map, "Vanilla", defaultValue))
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmptyMapIfEmpty() {
    Assert.assertEquals(emptyMap<String, String>(), convertActionParameterArrayToMap(null))
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmptyMapIfNoParamData() {
    val array = arrayOf(ActionParameter(key = "k", value = "v"))
    Assert.assertEquals(emptyMap<String, String>(), convertActionParameterArrayToMap(array))
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmtpyMapIfArrayIsEmpty() {
    val array = emptyArray<ActionParameter>()
    Assert.assertEquals(emptyMap<String, String>(), convertActionParameterArrayToMap(array))
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnEmtpyMapValue() {
    val array =
      arrayOf(ActionParameter(key = "k", value = "", paramType = ActionParameterType.PARAMDATA))
    Assert.assertEquals("", convertActionParameterArrayToMap(array)["k"])
  }

  @Test
  fun testConvertActionParameterArrayToMapShouldReturnMapIfParamData() {
    val array =
      arrayOf(ActionParameter(key = "k", value = "v", paramType = ActionParameterType.PARAMDATA))
    Assert.assertEquals(mapOf("k" to "v"), convertActionParameterArrayToMap(array))
  }
}
