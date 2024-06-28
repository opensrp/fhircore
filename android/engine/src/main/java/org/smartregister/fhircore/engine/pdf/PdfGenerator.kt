package org.smartregister.fhircore.engine.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * PdfGenerator creates PDF files from HTML content using Android's WebView and PrintManager.
 * Must be initialized on the Main thread.
 */
class PdfGenerator {

    private var mWebView: WebView? = null

    /**
     * Generates a PDF file from the provided HTML content.
     *
     * This method loads the given HTML content into a WebView, creates a print adapter from
     * the WebView, and uses the PrintManager to generate a PDF document with the specified title.
     *
     * Example usage:
     * ```
     * val pdfGenerator = PdfGenerator(context)
     * val htmlContent = "<html><body><h1>Hello, World!</h1></body></html>"
     * pdfGenerator.generatePdfWithHtml(htmlContent, "SamplePDF")
     * ```
     *
     * @param context Application context for initializing WebView and PrintManager.
     * @param html The HTML content to be converted into a PDF.
     * @param pdfTitle The title of the PDF document.
     */
    fun generatePdfWithHtml(context: Context, html: String, pdfTitle: String, onPdfPrinted: () -> Unit) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                printPdf(context, view, pdfTitle)
                mWebView = null
                onPdfPrinted.invoke()
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        mWebView = webView
    }

    private fun printPdf(context: Context, view: WebView, pdfTitle: String) {
        val printAdapter = view.createPrintDocumentAdapter(pdfTitle)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            pdfTitle,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }
}