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
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PdfGeneratorTest {

  @Mock private lateinit var context: Context

  @Mock private lateinit var printManager: PrintManager

  @Mock private lateinit var webView: WebView

  @Mock private lateinit var printDocumentAdapter: PrintDocumentAdapter

  private lateinit var pdfGenerator: PdfGenerator

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(context.getSystemService(Context.PRINT_SERVICE)).thenReturn(printManager)
    pdfGenerator = PdfGenerator(context, webView) // Inject the mock webView
  }

  @Test
  fun testGeneratePdfWithHtml() {
    val htmlContent = "<html><body><h1>Hello, World!</h1></body></html>"
    val pdfTitle = "SamplePDF"

    `when`(webView.createPrintDocumentAdapter(pdfTitle)).thenReturn(printDocumentAdapter)

    pdfGenerator.generatePdfWithHtml(htmlContent, pdfTitle)

    verify(webView).loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    verify(webView).createPrintDocumentAdapter(pdfTitle)
    verify(printManager).print(eq(pdfTitle), eq(printDocumentAdapter), eq(null))
  }
}
