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

package org.smartregister.fhircore.engine.util.extension

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.logicalId
import com.google.android.fhir.workflow.FhirOperator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.RelatedArtifact
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class FhirEngineExtensionTest : RobolectricTest() {

  private val fhirEngine: FhirEngine = mockk()

  private lateinit var knowledgeManager: KnowledgeManager
  private lateinit var fhirOperator: FhirOperator

  @Before
  fun setUp() {
    knowledgeManager =
      KnowledgeManager.create(ApplicationProvider.getApplicationContext(), inMemory = true)
    val fhirContext = FhirContext(FhirVersionEnum.R4)

    fhirOperator =
      FhirOperator.Builder(ApplicationProvider.getApplicationContext())
        .fhirEngine(fhirEngine)
        .fhirContext(fhirContext)
        .knowledgeManager(knowledgeManager)
        .build()
  }

  @Test
  fun searchCompositionByIdentifier() = runBlocking {
    coEvery { fhirEngine.search<Composition>(any()) } returns
      listOf(SearchResult(resource = Composition().apply { id = "123" }, null, null))

    val result = fhirEngine.searchCompositionByIdentifier("appId")

    coVerify { fhirEngine.search<Composition>(any()) }

    Assert.assertEquals("123", result!!.logicalId)
  }

  @Test
  fun testLoadLibraryAtPathNullLibrary() {
    coEvery { fhirEngine.search<Library>(any()) } returns listOf()

    runBlocking { fhirEngine.loadLibraryAtPath(fhirOperator, "") }

    coVerify { fhirEngine.search<Library>(any()) }
  }

  @Test
  fun testLoadLibraryAtPathReturnedLibrary() {
    val library =
      Library().apply {
        id = "123"
        relatedArtifact =
          listOf(
            RelatedArtifact().apply {
              type = RelatedArtifact.RelatedArtifactType.DEPENDSON
              resource = "Library/456"
            },
            RelatedArtifact().apply { type = RelatedArtifact.RelatedArtifactType.CITATION },
          )
      }

    coEvery { fhirEngine.search<Library>(any()) } returns
      listOf(SearchResult(resource = library, null, null)) andThenAnswer
      {
        emptyList()
      }

    runBlocking { fhirEngine.loadLibraryAtPath(fhirOperator, "path") }

    coVerify { fhirEngine.search<Library>(any()) }
  }

  @Test
  fun testLoadCqlLibraryBundleNotUrl() {
    val measurePath = "path"
    val measure = Measure().apply { id = "123" }

    coEvery { fhirEngine.get(any(), measurePath) } returns measure

    runBlocking { fhirEngine.loadCqlLibraryBundle(fhirOperator, measurePath) }

    coVerify { fhirEngine.get(any(), measurePath) }
  }

  @Test
  fun testLoadCqlLibraryBundleUrl() {
    val measurePath = "http://example.com"
    val measure =
      Measure().apply {
        id = "123"
        library = listOf(CanonicalType().apply { value = "Library/456" })
        relatedArtifact =
          listOf(
            RelatedArtifact().apply { type = RelatedArtifact.RelatedArtifactType.DEPENDSON },
          )
      }

    coEvery { fhirEngine.search<Measure>(any()) } returns
      listOf(SearchResult(resource = measure, null, null))

    runBlocking { fhirEngine.loadCqlLibraryBundle(fhirOperator, measurePath) }

    coVerify { fhirEngine.search<Measure>(any()) }
  }
}
