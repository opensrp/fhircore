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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import org.hl7.fhir.r4.model.Binary
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import retrofit2.Call
import retrofit2.Response

@HiltAndroidTest
@Ignore("Ignore - to be fixed")
class ReferenceAttachmentResolverTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutinesTestRule = CoroutineTestRule()

  private val fhirService: FhirResourceService = mockk()

  @Inject lateinit var referenceAttachmentResolver: ReferenceAttachmentResolver

  @Inject lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testResolveBinaryResourceShouldCallFhirEngineLoadAndReturnBinary() =
    coroutinesTestRule.runBlockingTest {
      val expectedBinaryResource = Binary().apply { id = "binaryId" }
      fhirEngine.save(expectedBinaryResource)
      val actualBinaryResource =
        referenceAttachmentResolver.resolveBinaryResource(
          "https://fhir-server.org/Binary/sample-binary-image"
        )
      Assert.assertEquals(expectedBinaryResource, actualBinaryResource)
    }

  @Test
  fun testResolveImageUrlShouldCallFetchImage() {
    val imageUrl = "https://image-server.com/8929839"

    val okHttpCall = mockk<Call<ResponseBody?>>()
    val mockResponse = Response.success<ResponseBody?>(null)

    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(any()) } returns okHttpCall

    runBlocking { referenceAttachmentResolver.resolveImageUrl(imageUrl) }

    verify { fhirService.fetchImage(imageUrl) }
  }

  @Test
  fun testResolveImageUrlShouldReturnNullWhenBodyIsNull() {
    val imageUrl = "https://image-server.com/8929839"
    val okHttpCall = mockk<Call<ResponseBody?>>()

    val mockResponse = Response.success<ResponseBody?>(null)

    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(imageUrl) } returns okHttpCall

    val bitmap: Bitmap?
    runBlocking { bitmap = referenceAttachmentResolver.resolveImageUrl(imageUrl) }

    Assert.assertNull(bitmap)
  }

  @Test
  fun testResolveImageUrlShouldReturnDecodeAndReturnWhenServiceReturnsBody() {
    val imageUrl = "https://image-server.com/8929839"
    val okHttpCall = mockk<Call<ResponseBody?>>()
    val mockResponseBody: ResponseBody = spyk(FakeResponseBody())
    val mockResponse = Response.success<ResponseBody?>(mockResponseBody)

    every { mockResponseBody.byteStream() } returns
      (ByteArrayInputStream(
        "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7".toByteArray(
          Charset.forName("UTF-8")
        )
      ))
    every { okHttpCall.execute() } returns mockResponse
    every { fhirService.fetchImage(imageUrl) } returns okHttpCall

    val bitmap: Bitmap?
    runBlocking { bitmap = referenceAttachmentResolver.resolveImageUrl(imageUrl) }

    Assert.assertNotNull(bitmap)
  }

  class FakeResponseBody : ResponseBody() {

    override fun contentLength(): Long = 0L

    override fun contentType(): MediaType? = null

    override fun source(): BufferedSource = mockk()
  }
}
