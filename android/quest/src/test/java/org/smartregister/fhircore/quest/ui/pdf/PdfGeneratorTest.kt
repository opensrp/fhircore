package org.smartregister.fhircore.quest.ui.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.mockk.*
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
        any<PrintAttributes>()
      )
    }
  }
}
