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

package org.smartregister.fhircore.engine.util

import android.content.Context
import android.content.res.AssetManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil.ASSET_BASE_PATH_RESOURCES

class FileUtilTest {

  var libraryData = ""

  @MockK lateinit var context: Context

  @MockK lateinit var assetManager: AssetManager

  lateinit var inputStream: InputStream

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun getPropertyTest() {
    val fileName = ASSET_BASE_PATH_RESOURCES + "test/resources/fileutil/cql_configs.properties"
    val file = File(fileName)
    inputStream = FileInputStream(file)
    every { context.assets } returns assetManager
    every { assetManager.open(any()) } returns inputStream
    val smartRegisterBaseUrl =
      FileUtil.getProperty("smart_register_base_url", context, "cql_configs.properties")
    Assert.assertEquals("https://fhir.labs.smartregister.org/fhir/", smartRegisterBaseUrl)
  }

  @Test
  fun readJsonFileTest() {
    libraryData = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/library.json")
    Assert.assertNotNull(libraryData)
  }

  @Test
  fun recurseFoldersTest() {
    val baseTestPathMeasureAssets =
      System.getProperty("user.dir")!! +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/measureevaluator/"
    val filePatientAssetDir = File(baseTestPathMeasureAssets)
    val fileListString = FileUtil.recurseFolders(filePatientAssetDir)
    Assert.assertNotNull(fileListString)
  }

  @Test
  fun recurseFoldersTestCatchesThrow() {
    val fileDir = mockk<File>()
    every { fileDir.listFiles() }.throws(IOException())
    val fileListString = FileUtil.recurseFolders(fileDir)
    Assert.assertEquals(mutableListOf<String>(), fileListString)
  }

  @Test
  fun testWriteFileOnInternalStorage() {
    val exampleFileName = "example.json"
    val baseDir =
      System.getProperty("user.dir")!! +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/libraryevaluator/"

    val completeFile = baseDir + File.separator + exampleFileName

    every { context.filesDir } returns File(baseDir)

    FileUtil.writeFileOnInternalStorage(context, exampleFileName, "hello", "")

    val f1 = File(completeFile)
    Assert.assertNotNull(f1)
    f1.delete()

    val baseDir2 =
      System.getProperty("user.dir")!! +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/example/"

    val fileBaseDir2 = File(baseDir2)
    val completeFile2 = baseDir2 + File.separator + exampleFileName

    every { context.filesDir } returns fileBaseDir2

    FileUtil.writeFileOnInternalStorage(context, exampleFileName, "hello", "")

    val f2 = File(completeFile2)
    Assert.assertNotNull(f2)
    f2.delete()
    fileBaseDir2.delete()
  }

  @Test
  fun testWriteFileOnInternalStorageWithEmptyBody() {
    val exampleFileName = "example.json"
    val baseDir =
      System.getProperty("user.dir")!! +
        File.separator +
        "src" +
        File.separator +
        File.separator +
        "test/resources/cql/libraryevaluator/"

    val completeFile = baseDir + File.separator + exampleFileName

    every { context.filesDir } returns File(baseDir)

    FileUtil.writeFileOnInternalStorage(context, exampleFileName, dirName = "")

    val f1 = File(completeFile)
    Assert.assertNotNull(f1)
    f1.delete()
  }

  @Test
  fun testReadFileFromInternalStorage() {
    every { context.filesDir } returns
      File(
        System.getProperty("user.dir")!! +
          File.separator +
          "src" +
          File.separator +
          File.separator +
          "test/resources/cql/libraryevaluator/"
      )

    val fileListString = FileUtil.readFileFromInternalStorage(context, "library.json", "")

    Assert.assertNotNull(fileListString)
  }
}
