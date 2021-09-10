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

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class LibraryEvaluatorTest {
  var evaluator: LibraryEvaluator? = null
  var libraryData = ""
  var helperData = ""
  var valueSetData = ""
  var testData = ""
  var result = ""
  var evaluatorId = "ANCRecommendationA2"
  var context = "Patient"
  var contextLabel = "mom-with-anemia"
  @Before
  fun setUp() {
    try {
      libraryData = readJsonFile(ASSET_BASE_PATH + "library.json")
      helperData = readJsonFile(ASSET_BASE_PATH + "helper.json")
      valueSetData = readJsonFile(ASSET_BASE_PATH + "valueSet.json")
      testData = readJsonFile(ASSET_BASE_PATH + "patient.json")
      result = readJsonFile(ASSET_BASE_PATH + "result.json")
    } catch (e: IOException) {
      Timber.e(e, e.message);
    }
  }

  @Test
  fun runCql() {
    evaluator = LibraryEvaluator()
    val auxResult =
      evaluator!!.runCql(
        libraryData,
        helperData,
        valueSetData,
        testData,
        evaluatorId,
        context,
        contextLabel
      )
    Assert.assertEquals(result, auxResult)
  }

  @Throws(IOException::class)
  private fun readJsonFile(filename: String): String {
    val br = BufferedReader(InputStreamReader(FileInputStream(filename)))
    val sb = StringBuilder()
    var line = br.readLine()
    while (line != null) {
      sb.append(line)
      line = br.readLine()
    }
    return sb.toString()
  }

  companion object {
    val ASSET_BASE_PATH =
      (System.getProperty("user.dir") +
        File.separator +
        "src" +
        File.separator +
        "test" +
        File.separator +
        "resources" +
        File.separator)
  }
}
