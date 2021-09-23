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

import java.io.IOException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil
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

  val fileUtil = FileUtil()
  @Before
  fun setUp() {
    try {
      libraryData = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/library.json")
      helperData = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/helper.json")
      valueSetData = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/valueSet.json")
      testData = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/patient.json")
      result = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/result.json")
      evaluator = LibraryEvaluator()
    } catch (e: IOException) {
      Timber.e(e, e.message)
    }
  }

  @Test
  fun runCqlTest() {
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

  @Test
  fun processCQLPatientBundleTest() {
    var results = evaluator!!.processCQLPatientBundle(testData)
    Assert.assertNotNull(results)
  }
}
