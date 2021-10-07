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

import java.io.File
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil

class MeasureEvaluatorTest {

  var baseTestPathMeasureAssets =
    System.getProperty("user.dir") +
      File.separator +
      "src" +
      File.separator +
      File.separator +
      "test/resources/cql/measureevaluator/"
  var patientAssetsDir = baseTestPathMeasureAssets + "first-contact"
  var libraryFilePath = "test/resources/cql/measureevaluator/library/ANCIND01-bundle.json"

  @Test
  @Ignore("This test causes java.lang.OutOfMemoryError: Java heap space")
  fun runMeasureEvaluate() {
    var filePatientAssetDir = File(patientAssetsDir)
    var fileUtil = FileUtil()
    var fileListString = fileUtil.recurseFolders(filePatientAssetDir)
    var patientResources: ArrayList<String> = ArrayList()
    for (f in fileListString) {
      patientResources.add(fileUtil.readJsonFile(f))
    }
    var measureEvaluator = MeasureEvaluator()
    var measureReport =
      measureEvaluator.runMeasureEvaluate(
        fileUtil.readJsonFile(libraryFilePath),
        patientResources,
        "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
        "2020-01-01",
        "2020-01-31",
        "subject",
        "patient-charity-otala-1"
      )

    Assert.assertNotNull(measureReport)
  }
}
