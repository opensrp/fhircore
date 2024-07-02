package org.smartregister.fhircore.quest.ui.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jetbrains.annotations.VisibleForTesting

/**
 * PdfGenerator creates PDF files from HTML content using Android's WebView and PrintManager.
 * Must be initialized on the Main thread.
 *
 * @param context Application context for initializing WebView and PrintManager.
 * @param webView WebView instance for loading HTML content (Visible for testing).
 */
class PdfGenerator(
  private val context: Context,
  @VisibleForTesting private val webView: WebView = WebView(context)
) {

  private var mWebView: WebView? = null
  private val printManager: PrintManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

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
   * @param onPdfPrinted Callback to be invoked when the PDF is printed.
   */
  fun generatePdfWithHtml(html: String, pdfTitle: String, onPdfPrinted: () -> Unit) {
    webView.webViewClient = object : WebViewClient() {

      override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

      override fun onPageFinished(view: WebView, url: String) {
        printPdf(view, pdfTitle)
        mWebView = null
        onPdfPrinted.invoke()
      }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    mWebView = webView
  }

  private fun printPdf(view: WebView, pdfTitle: String) {
    val printAdapter = view.createPrintDocumentAdapter(pdfTitle)
    printManager.print(
      pdfTitle,
      printAdapter,
      PrintAttributes.Builder().build(),
    )
  }
}
