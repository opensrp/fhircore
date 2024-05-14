package org.smartregister.fhircore.engine.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView

/**
 * PdfGenerator is a utility class for generating PDF files from HTML content.
 *
 * This class utilizes Android's WebView and PrintManager to render HTML content and
 * print it as a PDF document.
 *
 * @param context The application context used to initialize WebView and PrintManager.
 */
class PdfGenerator(context: Context) {

    private val webView = WebView(context)
    private val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

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
     * @param html The HTML content to be converted into a PDF.
     * @param pdfTitle The title of the PDF document.
     */
    fun generatePdfWithHtml(html: String, pdfTitle: String) {
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        val printAdapter = webView.createPrintDocumentAdapter(pdfTitle)
        printManager.print(
            pdfTitle,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }
}