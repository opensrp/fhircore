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

package org.smartregister.fhircore.engine.cql

import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class FhirLibraryLoaderTest : RobolectricTest() {

  @Test
  fun testJsonReaderShouldMapLibrary() {
    val elmJson = "cql/g6pdlibraryevaluator/library-elm.json".readFile()
    val result = FhirLibraryLoader(ModelManager(), listOf()).readJxson(elmJson.byteInputStream())

    Assert.assertEquals(1, result.annotation.size)
    Assert.assertTrue(result.usings.def.isNotEmpty())
    Assert.assertTrue(result.statements.def.isNotEmpty())
  }

  @Test
  fun testTranslatorOptionShorldMatchShouldReturnTrue() {
    val loader = FhirLibraryLoader(ModelManager(), listOf())

    Assert.assertTrue(loader.translatorOptionsMatch(Library()))
  }
}
