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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
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
  @Ignore("Fails with 'java.lang.OutOfMemoryError: Java heap space' on local and CI as well")
  fun runMeasureEvaluate() {
    val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    val parser = fhirContext.newJsonParser()!!

    val filePatientAssetDir = File(patientAssetsDir)
    val fileUtil = FileUtil()
    val fileListString = fileUtil.recurseFolders(filePatientAssetDir)
    val patientResources: ArrayList<String> = ArrayList()
    for (f in fileListString) {
      patientResources.add(fileUtil.readJsonFile(f))
    }

    val resources = ArrayList<IBaseResource>()
    for (r in patientResources) {
      val patientDataStream: InputStream = ByteArrayInputStream(r.toByteArray())
      val patientData = parser.parseResource(patientDataStream) as IBaseBundle
      resources.add(patientData)
    }

    val libraryStream: InputStream =
      ByteArrayInputStream(fileUtil.readJsonFile(libraryFilePath).toByteArray())
    val library = parser.parseResource(libraryStream) as IBaseBundle

    val measureEvaluator = MeasureEvaluator()
    val measureReport =
      measureEvaluator.runMeasureEvaluate(
        resources,
        library,
        fhirContext,
        "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
        "2020-01-01",
        "2020-01-31",
        "subject",
        "patient-charity-otala-1"
      )

    Assert.assertNotNull(measureReport)
  }
}
