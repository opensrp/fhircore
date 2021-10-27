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

package org.smartregister.fhircore.quest

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import org.hl7.fhir.r4.model.Binary
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow
import retrofit2.Call
import retrofit2.Response

@Config(shadows = [QuestApplicationShadow::class])
class ReferenceAttachmentResolverTest : RobolectricTest() {

  lateinit var referenceAttachmentResolverTest: ReferenceAttachmentResolver

  @Before
  fun setUp() {
    referenceAttachmentResolverTest =
      spyk(
        ReferenceAttachmentResolver(ApplicationProvider.getApplicationContext<QuestApplication>())
      )
  }

  @Test
  fun testResolveBinaryResourceShouldCallFhirEngineLoadAndReturnBinary() {
    val fhirEngine = mockk<FhirEngine>()
    val expectedBinaryResource = Binary()

    ReflectionHelpers.setField(
      QuestApplication.getContext(),
      "fhirEngine\$delegate",
      lazy { fhirEngine }
    )

    coEvery { fhirEngine.load(Binary::class.java, "sample-binary-image") } returns
      expectedBinaryResource

    val actualBinaryResource: Binary?
    runBlocking {
      actualBinaryResource =
        referenceAttachmentResolverTest.resolveBinaryResource(
          "https://fhir-server.org/Binary/sample-binary-image"
        )
    }

    coVerify { fhirEngine.load(Binary::class.java, "sample-binary-image") }
    Assert.assertEquals(expectedBinaryResource, actualBinaryResource)
  }

  @Test
  fun testResolveImageUrlShouldCallFetchImage() {
    val imageUrl = "https://image-server.com/8929839"
    val fhirService = mockk<FhirResourceService>()
    val okHttpCall = mockk<Call<ResponseBody?>>()
    val mockResponse = Response.success<ResponseBody?>(null)

    every { referenceAttachmentResolverTest.getFhirService() } returns fhirService
    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(any()) } returns okHttpCall

    runBlocking { referenceAttachmentResolverTest.resolveImageUrl(imageUrl) }

    verify { fhirService.fetchImage(imageUrl) }
  }

  @Test
  fun testResolveImageUrlShouldReturnNullWhenBodyIsNull() {
    val imageUrl = "https://image-server.com/8929839"
    val fhirService = mockk<FhirResourceService>()
    val okHttpCall = mockk<Call<ResponseBody?>>()

    val mockResponse = Response.success<ResponseBody?>(null)

    every { referenceAttachmentResolverTest.getFhirService() } returns fhirService
    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(imageUrl) } returns okHttpCall

    val bitmap: Bitmap?
    runBlocking { bitmap = referenceAttachmentResolverTest.resolveImageUrl(imageUrl) }

    Assert.assertNull(bitmap)
  }

  @Test
  fun testResolveImageUrlShouldReturnDecodeAndReturnWhenServiceReturnsBody() {
    val imageUrl = "https://image-server.com/8929839"
    val fhirService = mockk<FhirResourceService>()
    val okHttpCall = mockk<Call<ResponseBody?>>()
    val mockResponseBody: ResponseBody = spyk(FakeResponseBody())
    val mockResponse = Response.success<ResponseBody?>(mockResponseBody)

    every { referenceAttachmentResolverTest.getFhirService() } returns fhirService
    every { mockResponseBody.byteStream() } returns
      (ByteArrayInputStream(
        "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7".toByteArray(
          Charset.forName("UTF-8")
        )
      ))
    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(imageUrl) } returns okHttpCall

    val bitmap: Bitmap?
    runBlocking { bitmap = referenceAttachmentResolverTest.resolveImageUrl(imageUrl) }

    Assert.assertNotNull(bitmap)
  }

  @Test
  fun testGetContext() {
    Assert.assertEquals(
      ApplicationProvider.getApplicationContext(),
      referenceAttachmentResolverTest.context
    )
  }

  class FakeResponseBody : ResponseBody() {

    override fun contentLength(): Long = 0L

    override fun contentType(): MediaType? = null

    override fun source(): BufferedSource = mockk()
  }
}
