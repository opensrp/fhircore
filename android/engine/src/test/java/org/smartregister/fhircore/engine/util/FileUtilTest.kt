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

package org.smartregister.fhircore.engine.util

import android.content.Context
import android.content.res.AssetManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil.Companion.ASSET_BASE_PATH_RESOURCES

class FileUtilTest {

  var libraryData = ""
  lateinit var fileUtil: FileUtil

  @MockK lateinit var context: Context

  @MockK lateinit var assetManager: AssetManager

  lateinit var inputStream: InputStream

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    fileUtil = FileUtil()
  }

  @Test
  fun getPropertyTest() {
    val fileName = ASSET_BASE_PATH_RESOURCES + "test/resources/fileutil/cql_configs.properties"
    val file = File(fileName)
    inputStream = FileInputStream(file)
    every { context.getAssets() } returns assetManager
    every { assetManager.open(any()) } returns inputStream
    val smartRegisterBaseUrl =
      fileUtil.getProperty("smart_register_base_url", context, "cql_configs.properties")
    Assert.assertEquals("https://fhir.labs.smartregister.org/fhir/", smartRegisterBaseUrl)
  }

  @Test
  fun readJsonFileTest() {
    libraryData = fileUtil.readJsonFile("test/resources/cql/libraryevaluator/library.json")
    Assert.assertNotNull(libraryData)
  }

  @Test
  fun recurseFoldersTest() {
    val baseTestPathMeasureAssets =
      System.getProperty("user.dir") +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/measureevaluator/"
    val patientAssetsDir = baseTestPathMeasureAssets + "first-contact"
    val filePatientAssetDir = File(patientAssetsDir)
    val fileUtil = FileUtil()
    val fileListString = fileUtil.recurseFolders(filePatientAssetDir)
    Assert.assertNotNull(fileListString)
  }

  @Test
  fun testWriteFileOnInternalStorage() {
    val exampleFileName = "example.json"
    val baseDir =
      System.getProperty("user.dir") +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/libraryevaluator/"

    val completeFile = baseDir + File.separator + exampleFileName

    every { context.getFilesDir() } returns File(baseDir)

    fileUtil.writeFileOnInternalStorage(context, exampleFileName, "hello", "")

    val f1 = File(completeFile)
    Assert.assertNotNull(f1)
    f1.delete()

    val baseDir2 =
      System.getProperty("user.dir") +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/example/"

    var fileBaseDir2 = File(baseDir2)
    val completeFile2 = baseDir2 + File.separator + exampleFileName

    every { context.getFilesDir() } returns fileBaseDir2

    fileUtil.writeFileOnInternalStorage(context, exampleFileName, "hello", "")

    val f2 = File(completeFile2)
    Assert.assertNotNull(f2)
    f2.delete()
    fileBaseDir2.delete()
  }

  @Test
  fun testReadFileFromInternalStorage() {
    every { context.getFilesDir() } returns
      File(
        System.getProperty("user.dir") +
          File.separator +
          "src" +
          File.separator +
          File.separator +
          "test/resources/cql/libraryevaluator/"
      )

    val fileListString = fileUtil.readFileFromInternalStorage(context, "library.json", "")

    Assert.assertNotNull(fileListString)
  }
}
