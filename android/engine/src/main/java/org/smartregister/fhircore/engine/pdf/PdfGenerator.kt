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

/**
 * PdfGenerator creates PDF files from HTML content using Android's WebView and PrintManager.
 * Must be initialized on the Main thread.
 *
 * @param context Application context for initializing WebView and PrintManager.
 */
class PdfGenerator(context: Context) {

  private val webView = WebView(context)
  private val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

  /**
   * Generates a PDF file from the provided HTML content.
   *
   * This method loads the given HTML content into a WebView, creates a print adapter from the
   * WebView, and uses the PrintManager to generate a PDF document with the specified title.
   *
   * Example usage:
   * ```
   * val pdfGenerator = PdfGenerator(context)
   * val htmlContent = "<html><body><h1>Hello, World!</h1></body></html>"
   * pdfGenerator.generatePdfWithHtml(htmlContent, "SamplePDF")
   * ```
   *
   * @param html The HTML content to be converted into a PDF.
   * @param pdfTitle The title of the PDF document.
   */
  fun generatePdfWithHtml(html: String, pdfTitle: String) {
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    val printAdapter = webView.createPrintDocumentAdapter(pdfTitle)
    printManager.print(
      pdfTitle,
      printAdapter,
      PrintAttributes.Builder().build(),
    )
  }
}
