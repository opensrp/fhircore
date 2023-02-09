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

package org.smartregister.fhircore.engine.cql

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.FileUtil

class CqlLibraryHelperTest : RobolectricTest() {

  private lateinit var fhirContext: FhirContext
  private lateinit var parser: IParser
  private lateinit var cqlLibraryHelper: CqlLibraryHelper

  @Before
  fun setUp() {
    mockkStatic(FhirContext::class)
    mockkObject(FileUtil)

    fhirContext = mockk()
    parser = mockk()

    every { FhirContext.forCached(any()) } returns fhirContext
    every { fhirContext.newJsonParser() } returns parser

    cqlLibraryHelper = CqlLibraryHelper(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testLoadMeasureEvaluateLibraryShouldReturnBaseBundle() {

    val bundle =
      Bundle().apply {
        addEntry().apply { resource = Patient().apply { addName().apply { addGiven("Robin") } } }
      }

    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns ""
    every { parser.parseResource(any<ByteArrayInputStream>()) } returns bundle

    val result = cqlLibraryHelper.loadMeasureEvaluateLibrary("", "") as Bundle

    Assert.assertEquals(1, result.entry.size)
    Assert.assertEquals(
      "Robin",
      (result.entryFirstRep.resource as Patient).nameFirstRep.givenAsSingleString
    )

    verify(exactly = 1) { FileUtil.readFileFromInternalStorage(any(), any(), any()) }
    verify(exactly = 1) { parser.parseResource(any<ByteArrayInputStream>()) }

    val sameResult = cqlLibraryHelper.loadMeasureEvaluateLibrary("", "") as Bundle

    Assert.assertEquals(1, sameResult.entry.size)
    Assert.assertEquals(
      "Robin",
      (sameResult.entryFirstRep.resource as Patient).nameFirstRep.givenAsSingleString
    )

    verify(exactly = 0, inverse = true) {
      FileUtil.readFileFromInternalStorage(any(), any(), any())
    }
    verify(exactly = 0, inverse = true) { parser.parseResource(any<ByteArrayInputStream>()) }
  }

  @Test
  fun testWriteMeasureEvaluateLibraryDataShouldWriteFileInternalStorage() {

    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit

    cqlLibraryHelper.writeMeasureEvaluateLibraryData("test_data", "test_file", "test_dir")

    verify(exactly = 1) {
      FileUtil.writeFileOnInternalStorage(any(), "test_file", "test_data", "test_dir")
    }
  }

  @After
  fun afterTests() {
    unmockkAll()
  }
}
