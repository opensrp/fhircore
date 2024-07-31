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

package org.smartregister.fhircore.quest.ui.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PdfGeneratorTest {

  private lateinit var pdfGenerator: PdfGenerator
  private val mockContext = mockk<Context>(relaxed = true)
  private val mockPrintManager = mockk<PrintManager>(relaxed = true)
  private val mockWebView = mockk<WebView>(relaxed = true)
  private val mockPrintDocumentAdapter = mockk<PrintDocumentAdapter>(relaxed = true)

  @Before
  fun setUp() {
    every { mockContext.getSystemService(Context.PRINT_SERVICE) } returns mockPrintManager
    every { mockWebView.createPrintDocumentAdapter(any()) } returns mockPrintDocumentAdapter
    pdfGenerator = PdfGenerator(mockContext, mockWebView)
  }

  @Test
  fun testPdfIsPrintedWithCorrectParameters() {
    val pdfTitle = "SamplePDF"

    pdfGenerator.generatePdfWithHtml("<html></html>", pdfTitle) {}

    // Capture the WebViewClient that is set on the WebView
    val webViewClientSlot = slot<WebViewClient>()
    verify { mockWebView.webViewClient = capture(webViewClientSlot) }

    // Manually invoke the onPageFinished method to simulate page load completion
    webViewClientSlot.captured.onPageFinished(mockWebView, "url")

    // Verify createPrintDocumentAdapter and printManager.print calls
    verify { mockWebView.createPrintDocumentAdapter(pdfTitle) }
    verify {
      mockPrintManager.print(
        eq(pdfTitle),
        eq(mockPrintDocumentAdapter),
        any<PrintAttributes>(),
      )
    }
  }
}
