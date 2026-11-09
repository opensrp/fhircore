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

package org.smartregister.fhircore.quest.ui.questionnaire.attachment

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = Application::class)
class AttachmentItemViewHolderFactoryTest {

  private val uri = Uri.parse("content://test.provider/document/TEST_photo.jpg")
  private val resolver = mockk<ContentResolver>()
  private val context = mockk<Context> { every { contentResolver } returns resolver }

  /** The exceptions a content provider can throw for a file it can't give. */
  private val providerFailures =
    listOf(
      FileNotFoundException("gone"),
      SecurityException("permission revoked"),
      IllegalArgumentException("unknown uri"),
      IllegalStateException("provider closed"),
      UnsupportedOperationException("not supported"),
    )

  @Test
  fun testReadBytesFromUriReturnsTheBytesAndClosesTheStream() {
    val stream = ClosingStream(byteArrayOf(1, 2, 3))
    every { resolver.openInputStream(uri) } returns stream

    Assert.assertArrayEquals(byteArrayOf(1, 2, 3), context.readBytesFromUri(uri, 10))
    Assert.assertTrue(stream.closed)
  }

  @Test
  fun testReadBytesFromUriReturnsNullForAnEmptyOrMissingStream() {
    every { resolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(0))
    Assert.assertNull(context.readBytesFromUri(uri, 10))

    every { resolver.openInputStream(uri) } returns null
    Assert.assertNull(context.readBytesFromUri(uri, 10))
  }

  @Test
  fun testReadBytesFromUriReturnsNullWhenTheProviderFailsToOpen() {
    providerFailures.forEach { failure ->
      every { resolver.openInputStream(uri) } throws failure
      Assert.assertNull(failure.javaClass.simpleName, context.readBytesFromUri(uri, 10))
    }
  }

  @Test
  fun testReadBytesFromUriReturnsNullAndClosesTheStreamWhenTheReadFails() {
    val stream = ClosingStream(byteArrayOf(1), failure = IOException("read failed"))
    every { resolver.openInputStream(uri) } returns stream

    Assert.assertNull(context.readBytesFromUri(uri, 10))
    Assert.assertTrue(stream.closed)
  }

  @Test
  fun testReadBytesFromUriReadsAFileAtTheLimitWhole() {
    val file = ByteArray(20_000) { it.toByte() }
    every { resolver.openInputStream(uri) } returns ByteArrayInputStream(file)

    Assert.assertArrayEquals(file, context.readBytesFromUri(uri, 20_000))
  }

  @Test
  fun testReadBytesFromUriStopsOneBytePastTheLimitAndClosesTheStream() {
    val stream = ClosingStream(ByteArray(50_000) { it.toByte() })
    every { resolver.openInputStream(uri) } returns stream

    val bytes = context.readBytesFromUri(uri, 20_000)!!
    Assert.assertEquals(20_001, bytes.size)
    Assert.assertEquals(20_000.toByte(), bytes.last())
    // Not read whole and then cut, which would use the memory the limit is there to save
    Assert.assertEquals(20_001, stream.consumed)
    Assert.assertTrue(stream.closed)
  }

  @Test(expected = CancellationException::class)
  fun testReadBytesFromUriPassesCancellationOn() {
    every { resolver.openInputStream(uri) } throws CancellationException("cancelled")
    context.readBytesFromUri(uri, 10)
  }

  @Test
  fun testGetMimeTypeFromUriReturnsTheTypeOrAnyType() {
    every { resolver.getType(uri) } returns "image/jpeg"
    Assert.assertEquals("image/jpeg", context.getMimeTypeFromUri(uri))

    every { resolver.getType(uri) } returns null
    Assert.assertEquals("*/*", context.getMimeTypeFromUri(uri))
  }

  @Test
  fun testGetMimeTypeFromUriReturnsNullWhenTheProviderFails() {
    providerFailures.forEach { failure ->
      every { resolver.getType(uri) } throws failure
      Assert.assertNull(failure.javaClass.simpleName, context.getMimeTypeFromUri(uri))
    }
  }

  @Test
  fun testGetFileNameReturnsTheDisplayNameAndClosesTheCursor() {
    val cursor = nameCursor("TEST_consent.pdf")
    every { resolver.query(uri, any(), null, null, null) } returns cursor

    Assert.assertEquals("TEST_consent.pdf", context.getFileName(uri))
    Assert.assertTrue(cursor.isClosed)
  }

  @Test
  fun testGetFileNameFallsBackToTheUriWhenTheProviderHasNoName() {
    val noRow = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
    val noColumn = MatrixCursor(arrayOf(OpenableColumns.SIZE)).apply { addRow(arrayOf(1L)) }
    listOf(null, noRow, noColumn, nameCursor(null)).forEach { cursor ->
      every { resolver.query(uri, any(), null, null, null) } returns cursor
      Assert.assertEquals("TEST_photo.jpg", context.getFileName(uri))
      cursor?.let { Assert.assertTrue(it.isClosed) }
    }
  }

  @Test
  fun testGetFileNameFallsBackToTheUriWhenTheProviderFails() {
    providerFailures.forEach { failure ->
      every { resolver.query(uri, any(), null, null, null) } throws failure
      Assert.assertEquals(failure.javaClass.simpleName, "TEST_photo.jpg", context.getFileName(uri))
    }
  }

  @Test
  fun testGetFileNameFallsBackToTheUriWhenTheCursorFails() {
    val cursor =
      object : MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)) {
        override fun getString(column: Int): String = throw IllegalStateException("closed")
      }
    cursor.addRow(arrayOf("TEST_consent.pdf"))
    every { resolver.query(uri, any(), null, null, null) } returns cursor

    Assert.assertEquals("TEST_photo.jpg", context.getFileName(uri))
    Assert.assertTrue(cursor.isClosed)
  }

  @Test
  fun testGetFileNameIsEmptyWhenTheUriHasNoPathEither() {
    val bareUri = Uri.parse("content://test.provider")
    every { resolver.query(bareUri, any(), null, null, null) } returns null

    Assert.assertEquals("", context.getFileName(bareUri))
  }

  @Test(expected = CancellationException::class)
  fun testGetFileNamePassesCancellationOn() {
    every { resolver.query(uri, any(), null, null, null) } throws CancellationException("cancelled")
    context.getFileName(uri)
  }

  private fun nameCursor(name: String?) =
    MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(name)) }

  /**
   * Returns [bytes], or throws [failure] on the first read. Records how many bytes were read and
   * whether it was closed.
   */
  private class ClosingStream(bytes: ByteArray, private val failure: IOException? = null) :
    InputStream() {
    private val source = ByteArrayInputStream(bytes)
    var closed = false
    var consumed = 0

    override fun read(): Int {
      failure?.let { throw it }
      return source.read().also { if (it >= 0) consumed++ }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
      failure?.let { throw it }
      return source.read(b, off, len).also { if (it > 0) consumed += it }
    }

    override fun close() {
      closed = true
    }
  }

  @Test
  fun testIsAttachmentItemTakesAttachmentsButNotSignatures() {
    val photo =
      Questionnaire.QuestionnaireItemComponent().apply {
        type = Questionnaire.QuestionnaireItemType.ATTACHMENT
      }
    val signature =
      Questionnaire.QuestionnaireItemComponent().apply {
        type = Questionnaire.QuestionnaireItemType.ATTACHMENT
        addExtension(
          "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
          CodeableConcept(Coding("http://aidbox.io/questionnaire-itemControl", "signature", null)),
        )
      }
    val text =
      Questionnaire.QuestionnaireItemComponent().apply {
        type = Questionnaire.QuestionnaireItemType.STRING
      }

    Assert.assertTrue(isAttachmentItem(photo))
    Assert.assertFalse(isAttachmentItem(signature))
    Assert.assertFalse(isAttachmentItem(text))
  }
}
