package com.billbharo.domain.utils

import android.content.Context
import android.os.Environment
import com.billbharo.data.models.Invoice
import dagger.hilt.android.qualifiers.ApplicationContext
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A utility for generating professional, GST-compliant PDF invoices using the iText7 library.
 *
 * This class handles the entire PDF creation process, including:
 * - Setting up the document structure and layout.
 * - Adding shop and customer details.
 * - Creating a detailed table of invoice items.
 * - Calculating and displaying the GST breakdown (CGST/SGST) and totals.
 * - Saving the generated PDF to the device's external storage.
 *
 * @property context The application context, used for accessing file storage.
 */
@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SHOP_NAME = "Bill Bharo Store"
        private const val SHOP_ADDRESS = "Shop No. 123, Market Road, City - 400001"
        private const val SHOP_PHONE = "+91 98765 43210"
        private const val SHOP_GSTIN = "27AABCU9603R1ZM"
        private const val SHOP_EMAIL = "contact@billbharo.com"
    }

    /**
     * Generates a PDF for the given invoice and saves it to a file.
     *
     * @param invoice The [Invoice] object containing the data to be included in the PDF.
     * @return A [Result] object containing the absolute file path of the generated PDF on success,
     *         or an exception on failure.
     */
    fun generateInvoicePdf(invoice: Invoice): Result<String> {
        return try {
            val fileName = "Invoice_${invoice.invoiceNumber}.pdf"
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "BillBharo/Invoices"
            )

            // Create the directory if it doesn't exist
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Set document metadata
            pdfDocument.documentInfo.title = "Tax Invoice - ${invoice.invoiceNumber}"
            pdfDocument.documentInfo.author = SHOP_NAME

            // Build the PDF content by adding various sections
            addHeader(document)
            addShopDetails(document)
            addInvoiceDetails(document, invoice)
            addCustomerDetails(document, invoice)
            addItemsTable(document, invoice)
            addTaxSummary(document, invoice)
            addFooter(document)

            document.close()

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds the main "TAX INVOICE" header to the document.
     *
     * @param document The iText [Document] to which the header will be added.
     */
    private fun addHeader(document: Document) {
        val header = Paragraph("TAX INVOICE")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(20f)
            .setBold()
            .setMarginBottom(10f)
        document.add(header)

        val divider = Table(1)
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
            .addCell(
                Cell().add(Paragraph(""))
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(SolidBorder(ColorConstants.BLACK, 2f))
            )
        document.add(divider)
    }

    /**
     * Adds the shop's details (name, address, contact info, GSTIN) to the document.
     *
     * @param document The iText [Document] to which the details will be added.
     */
    private fun addShopDetails(document: Document) {
        val table = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)

        // Shop name
        table.addCell(
            Cell().add(Paragraph(SHOP_NAME).setBold().setFontSize(14f))
                .setBorder(Border.NO_BORDER)
        )
        table.addCell(Cell().add(Paragraph("")).setBorder(Border.NO_BORDER))

        // Address
        table.addCell(
            Cell().add(Paragraph(SHOP_ADDRESS).setFontSize(10f))
                .setBorder(Border.NO_BORDER)
        )
        table.addCell(Cell().add(Paragraph("")).setBorder(Border.NO_BORDER))

        // Contact details
        table.addCell(
            Cell().add(Paragraph("Phone: $SHOP_PHONE").setFontSize(10f))
                .setBorder(Border.NO_BORDER)
        )
        table.addCell(Cell().add(Paragraph("")).setBorder(Border.NO_BORDER))

        // GSTIN
        table.addCell(
            Cell().add(Paragraph("GSTIN: $SHOP_GSTIN").setBold().setFontSize(10f))
                .setBorder(Border.NO_BORDER)
        )
        table.addCell(Cell().add(Paragraph("")).setBorder(Border.NO_BORDER))

        document.add(table)
        document.add(
            Table(1).setWidth(UnitValue.createPercentValue(100f))
                .addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                )
                .setMarginTop(5f)
        )
    }

    /**
     * Adds the invoice number and date to the document.
     *
     * @param document The iText [Document] to which the details will be added.
     * @param invoice The [Invoice] object containing the data.
     */
    private fun addInvoiceDetails(document: Document, invoice: Invoice) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(invoice.timestamp)

        val table = Table(floatArrayOf(1f, 1f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)

        table.addCell(
            Cell().add(Paragraph("Invoice No: ").setBold().add(invoice.invoiceNumber))
                .setFontSize(10f)
                .setBorder(Border.NO_BORDER)
        )
        table.addCell(
            Cell().add(Paragraph("Date: ").setBold().add(dateString))
                .setFontSize(10f)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
        )

        document.add(table)
    }

    /**
     * Adds the customer's details ("Bill To" section) to the document.
     *
     * @param document The iText [Document] to which the details will be added.
     * @param invoice The [Invoice] object containing the customer data.
     */
    private fun addCustomerDetails(document: Document, invoice: Invoice) {
        val table = Table(1)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)
            .setBackgroundColor(DeviceRgb(240, 240, 240))

        table.addCell(
            Cell().add(Paragraph("Bill To:").setBold().setFontSize(12f))
                .setBorder(Border.NO_BORDER)
                .setPadding(5f)
        )

        val customerName = invoice.customerName ?: "Walk-in Customer"
        table.addCell(
            Cell().add(Paragraph(customerName).setFontSize(11f))
                .setBorder(Border.NO_BORDER)
                .setPadding(5f)
        )

        if (!invoice.customerPhone.isNullOrEmpty()) {
            table.addCell(
                Cell().add(Paragraph("Phone: ${invoice.customerPhone}").setFontSize(10f))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5f)
            )
        }

        document.add(table)
    }

    /**
     * Adds a table of the invoice items to the document.
     *
     * The table includes columns for item description, HSN code, quantity, rate, and total amount.
     *
     * @param document The iText [Document] to which the table will be added.
     * @param invoice The [Invoice] object containing the list of items.
     */
    private fun addItemsTable(document: Document, invoice: Invoice) {
        val table = Table(floatArrayOf(0.5f, 2f, 1f, 1f, 1f, 1.5f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(15f)

        // Table header
        val headerColor = DeviceRgb(0, 102, 204)
        val headers = arrayOf("#", "Item Description", "HSN", "Qty", "Rate", "Amount")

        headers.forEach { headerText ->
            table.addHeaderCell(
                Cell().add(Paragraph(headerText).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f)
            )
        }

        // Table rows for each item
        invoice.items.forEachIndexed { index, item ->
            table.addCell(Cell().add(Paragraph("${index + 1}")).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
            table.addCell(Cell().add(Paragraph("${item.name}\n(${item.unit})")).setFontSize(9f))
            table.addCell(Cell().add(Paragraph(item.hsnCode ?: "N/A")).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
            table.addCell(Cell().add(Paragraph(String.format("%.2f", item.quantity))).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
            table.addCell(Cell().add(Paragraph("₹${String.format("%.2f", item.rate)}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(9f))
            table.addCell(Cell().add(Paragraph("₹${String.format("%.2f", item.amount)}")).setTextAlignment(TextAlignment.RIGHT).setFontSize(9f).setBold())
        }

        document.add(table)
    }

    /**
     * Adds the tax summary section, including subtotal, CGST, SGST, and the final total amount.
     *
     * @param document The iText [Document] to which the summary will be added.
     * @param invoice The [Invoice] object containing the tax and total amounts.
     */
    private fun addTaxSummary(document: Document, invoice: Invoice) {
        val table = Table(floatArrayOf(3f, 1.5f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)

        // Subtotal
        table.addCell(Cell().add(Paragraph("Subtotal").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))
        table.addCell(Cell().add(Paragraph("₹${String.format("%.2f", invoice.subtotal)}").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))

        // CGST
        table.addCell(Cell().add(Paragraph("CGST (9%)").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))
        table.addCell(Cell().add(Paragraph("₹${String.format("%.2f", invoice.cgst)}").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))

        // SGST
        table.addCell(Cell().add(Paragraph("SGST (9%)").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))
        table.addCell(Cell().add(Paragraph("₹${String.format("%.2f", invoice.sgst)}").setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setFontSize(10f))

        // Divider
        table.addCell(Cell().setBorder(Border.NO_BORDER).setBorderTop(SolidBorder(ColorConstants.BLACK, 1f)))
        table.addCell(Cell().setBorder(Border.NO_BORDER).setBorderTop(SolidBorder(ColorConstants.BLACK, 1f)))

        // Total
        table.addCell(
            Cell().add(Paragraph("Total Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setFontSize(12f)
                .setBackgroundColor(DeviceRgb(240, 240, 240))
        )
        table.addCell(
            Cell().add(Paragraph("₹${String.format("%.2f", invoice.totalAmount)}").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setFontSize(12f)
                .setBackgroundColor(DeviceRgb(240, 240, 240))
        )

        // Payment mode
        table.addCell(
            Cell().add(Paragraph("Payment Mode").setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setFontSize(10f)
                .setPaddingTop(5f)
        )
        table.addCell(
            Cell().add(Paragraph(invoice.paymentMode.name).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setFontSize(10f)
                .setPaddingTop(5f)
        )

        document.add(table)
    }

    /**
     * Adds the footer section with a thank you message and other closing remarks.
     *
     * @param document The iText [Document] to which the footer will be added.
     */
    private fun addFooter(document: Document) {
        document.add(
            Paragraph("\nThank you for your business!")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setMarginTop(20f)
        )

        document.add(
            Paragraph("This is a computer-generated invoice and does not require a signature.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(8f)
                .setItalic()
                .setMarginTop(5f)
        )

        document.add(
            Paragraph("Terms & Conditions Apply | For any queries: $SHOP_EMAIL")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(7f)
                .setMarginTop(10f)
        )
    }
}
