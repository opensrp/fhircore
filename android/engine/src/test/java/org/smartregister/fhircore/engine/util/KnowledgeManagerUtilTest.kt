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

package org.smartregister.fhircore.engine.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import java.io.File
import org.hl7.fhir.r4.model.StructureMap
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class KnowledgeManagerUtilTest : RobolectricTest() {

  private lateinit var configService: AppConfigService
  private val context = ApplicationProvider.getApplicationContext<Context>()!!

  @Before
  fun setUp() {
    configService = AppConfigService(context)
  }

  @Test
  fun testWriteToFile() {
    val structureMap = StructureMap().apply { id = "structure-map-id" }

    val filePath =
      "${KnowledgeManagerUtil.KNOWLEDGE_MANAGER_ASSETS_SUBFOLDER}/StructureMap/structure-map-id.json"
    val absoluteFilePath = "${context.filesDir}/$filePath"

    val file = File(absoluteFilePath)
    Assert.assertFalse(file.exists())

    KnowledgeManagerUtil.writeToFile(filePath, structureMap, configService, context)

    Assert.assertTrue(file.exists())

    val savedStructureMap =
      FhirContext.forR4Cached().newJsonParser().parseResource(file.readText()) as StructureMap
    Assert.assertNotNull(savedStructureMap.url)
    Assert.assertEquals(
      "http://fake.base.url.com/StructureMap/structure-map-id",
      savedStructureMap.url,
    )
  }

  @After
  fun tearDown() {
    val testFile =
      File(
        "${context.filesDir}/${KnowledgeManagerUtil.KNOWLEDGE_MANAGER_ASSETS_SUBFOLDER}/StructureMap/structure-map-id.json",
      )
    if (testFile.exists()) {
      testFile.delete()
    }
  }
}
