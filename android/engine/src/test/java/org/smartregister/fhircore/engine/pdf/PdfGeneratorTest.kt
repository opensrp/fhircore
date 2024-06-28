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

package org.smartregister.fhircore.engine.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PdfGeneratorTest {

  @Mock private lateinit var mockContext: Context

  @Mock private lateinit var mockWebView: WebView

  @Mock private lateinit var mockPrintManager: PrintManager

  @Captor private lateinit var webViewClientCaptor: ArgumentCaptor<WebViewClient>

  private lateinit var pdfGenerator: PdfGenerator

  @Before
  fun setUp() {
    `when`(mockContext.getSystemService(Context.PRINT_SERVICE)).thenReturn(mockPrintManager)
    pdfGenerator = PdfGenerator(mockContext)
  }

  @Test
  fun testGeneratePdfWithHtml() {
    val htmlContent = "<html><body><h1>Hello, World!</h1></body></html>"
    val pdfTitle = "SamplePDF"
    val onPdfPrinted = mock(Runnable::class.java)

    doAnswer {
        val webView = it.getArgument<WebView>(0)
        webViewClientCaptor.value.onPageFinished(webView, "")
        null
      }
      .`when`(mockWebView)
      .loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

    pdfGenerator.generatePdfWithHtml(htmlContent, pdfTitle, onPdfPrinted::run)
    verify(mockWebView).webViewClient = webViewClientCaptor.capture()

    val printAdapter = mockWebView.createPrintDocumentAdapter(pdfTitle)
    verify(mockPrintManager).print(eq(pdfTitle), eq(printAdapter), any(PrintAttributes::class.java))

    verify(onPdfPrinted).run()
  }
}
