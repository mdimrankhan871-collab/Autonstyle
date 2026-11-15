package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.PackagingPurchase
import com.example.data.model.Product
import com.example.data.model.ProductPurchase
import com.example.data.model.ProductReturn
import com.example.domain.AccountingEngine
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ReportType(val title: String, val fileSlug: String) {
    SALES("Sales Report", "sales_report"),
    PURCHASES("Purchase Report", "purchase_report"),
    PRODUCT_STOCK("Product Stock Report", "product_stock_report"),
    PACKAGING_STOCK("Packaging Stock Report", "packaging_stock_report"),
    EXPENSES("Expense Report", "expense_report"),
    RETURNS("Return Report", "return_report"),
    PRODUCT_PROFIT("Product-wise Profit Report", "product_profit_report"),
    MONTHLY_PNL("Monthly Profit & Loss", "monthly_pnl_report")
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV", "csv", "text/csv"),
    EXCEL("Excel (.xlsx)", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("PDF", "pdf", "application/pdf")
}

data class ReportTable(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryLines: List<String>
)

object ReportBuilder {

    fun build(
        type: ReportType,
        products: List<Product>,
        productPurchases: List<ProductPurchase>,
        packagingItems: List<PackagingItem>,
        packagingPurchases: List<PackagingPurchase>,
        orders: List<Order>,
        returns: List<ProductReturn>,
        expenses: List<Expense>
    ): ReportTable {
        val generatedAt = "Generated: ${DhakaDateTime.formatDateTime(System.currentTimeMillis())} (Asia/Dhaka)"
        return when (type) {
            ReportType.SALES -> {
                val rows = orders.map { o ->
                    listOf(
                        o.orderDate,
                        o.invoiceNumber,
                        o.customerName,
                        o.statusEnum.label,
                        BdtFormatter.formatPlain(o.subtotal),
                        BdtFormatter.formatPlain(o.discountAmount),
                        BdtFormatter.formatPlain(o.returnedAmount),
                        BdtFormatter.formatPlain(o.totalAmount)
                    )
                }
                val activeSales = orders.filter { it.statusEnum != OrderStatus.CANCELLED }
                    .sumOf { (it.subtotal - it.discountAmount - it.returnedAmount).coerceAtLeast(0.0) }
                ReportTable(
                    title = "Autonstyle Sales Report",
                    subtitle = generatedAt,
                    headers = listOf("Date", "Invoice", "Customer", "Status", "Subtotal (BDT)", "Discount", "Returned", "Total"),
                    rows = rows,
                    summaryLines = listOf(
                        "Total Orders: ${orders.size}",
                        "Net Active Product Sales: ${BdtFormatter.format(activeSales, true)}"
                    )
                )
            }

            ReportType.PURCHASES -> {
                val prodRows = productPurchases.map { p ->
                    listOf(
                        p.purchaseDate,
                        "Product",
                        p.productName,
                        p.supplier,
                        p.quantity.toString(),
                        BdtFormatter.formatPlain(p.unitPrice),
                        BdtFormatter.formatPlain(p.totalCost)
                    )
                }
                val pkgRows = packagingPurchases.map { p ->
                    listOf(
                        p.purchaseDate,
                        "Packaging",
                        p.packagingName,
                        p.supplier,
                        p.quantity.toString(),
                        BdtFormatter.formatPlain(p.unitPrice),
                        BdtFormatter.formatPlain(p.totalCost)
                    )
                }
                val allRows = (prodRows + pkgRows).sortedByDescending { it.firstOrNull() ?: "" }
                val totalSpend = productPurchases.sumOf { it.totalCost } + packagingPurchases.sumOf { it.totalCost }
                ReportTable(
                    title = "Autonstyle Purchase Report",
                    subtitle = generatedAt,
                    headers = listOf("Date", "Type", "Item", "Supplier", "Qty", "Unit Cost", "Total Cost (BDT)"),
                    rows = allRows,
                    summaryLines = listOf(
                        "Purchase Entries: ${allRows.size}",
                        "Total Purchase Cost: ${BdtFormatter.format(totalSpend, true)}"
                    )
                )
            }

            ReportType.PRODUCT_STOCK -> {
                val rows = products.map { p ->
                    val status = when {
                        p.availableStock <= 0L -> "OUT OF STOCK"
                        p.availableStock <= p.minStockLevel -> "LOW STOCK"
                        else -> "IN STOCK"
                    }
                    listOf(
                        p.sku,
                        p.name,
                        p.category,
                        p.availableStock.toString(),
                        p.minStockLevel.toString(),
                        p.damagedStock.toString(),
                        BdtFormatter.formatPlain(p.purchasePrice),
                        BdtFormatter.formatPlain(p.availableStock * p.purchasePrice),
                        status
                    )
                }
                val totalVal = products.sumOf { it.availableStock * it.purchasePrice }
                ReportTable(
                    title = "Autonstyle Product Stock Report",
                    subtitle = generatedAt,
                    headers = listOf("SKU", "Product", "Category", "Available", "Min Level", "Damaged", "Unit Cost", "Stock Value", "Status"),
                    rows = rows,
                    summaryLines = listOf(
                        "Total SKUs: ${products.size} | Total Units: ${products.sumOf { it.availableStock }}",
                        "Total Inventory Value: ${BdtFormatter.format(totalVal, true)}"
                    )
                )
            }

            ReportType.PACKAGING_STOCK -> {
                val rows = packagingItems.map { p ->
                    val status = when {
                        p.availableQty <= 0L -> "OUT OF STOCK"
                        p.availableQty <= p.minStockLevel -> "LOW STOCK"
                        else -> "OK"
                    }
                    listOf(
                        p.name,
                        p.purchasedQty.toString(),
                        p.availableQty.toString(),
                        p.usedQty.toString(),
                        p.wastedQty.toString(),
                        p.minStockLevel.toString(),
                        BdtFormatter.formatPlain(p.purchasePrice),
                        BdtFormatter.formatPlain(p.availableQty * p.purchasePrice),
                        status
                    )
                }
                val pkgVal = packagingItems.sumOf { it.availableQty * it.purchasePrice }
                ReportTable(
                    title = "Autonstyle Packaging Stock Report",
                    subtitle = generatedAt,
                    headers = listOf("Packaging Item", "Purchased", "Available", "Used", "Wasted", "Min Stock", "Unit Cost", "Stock Value", "Status"),
                    rows = rows,
                    summaryLines = listOf(
                        "Packaging Materials: ${packagingItems.size} | Available Units: ${packagingItems.sumOf { it.availableQty }}",
                        "Packaging Stock Value: ${BdtFormatter.format(pkgVal, true)}"
                    )
                )
            }

            ReportType.EXPENSES -> {
                val rows = expenses.map { e ->
                    listOf(
                        e.expenseDate,
                        e.categoryEnum.label,
                        e.title,
                        BdtFormatter.formatPlain(e.amount),
                        e.notes
                    )
                }
                val total = expenses.sumOf { it.amount }
                ReportTable(
                    title = "Autonstyle Expense Report",
                    subtitle = generatedAt,
                    headers = listOf("Date", "Category", "Title", "Amount (BDT)", "Notes"),
                    rows = rows,
                    summaryLines = listOf(
                        "Expense Entries: ${expenses.size}",
                        "Total Recorded Expenses: ${BdtFormatter.format(total, true)}"
                    )
                )
            }

            ReportType.RETURNS -> {
                val rows = returns.map { r ->
                    listOf(
                        r.returnDate,
                        r.invoiceNumber,
                        r.productName,
                        r.returnedQty.toString(),
                        BdtFormatter.formatPlain(r.refundAmount),
                        BdtFormatter.formatPlain(r.returnCourierCharge),
                        if (!r.isReceived) "Not Received" else if (r.isResellable) "Resellable" else "Damaged",
                        r.reason
                    )
                }
                val totalRefund = returns.sumOf { it.refundAmount }
                val totalCourier = returns.sumOf { it.returnCourierCharge }
                ReportTable(
                    title = "Autonstyle Return Report",
                    subtitle = generatedAt,
                    headers = listOf("Date", "Invoice", "Product", "Qty", "Refund (BDT)", "Return Courier", "Condition", "Reason"),
                    rows = rows,
                    summaryLines = listOf(
                        "Total Returns: ${returns.size} | Units Returned: ${returns.sumOf { it.returnedQty }}",
                        "Total Refunded: ${BdtFormatter.format(totalRefund, true)} | Return Courier Fees: ${BdtFormatter.format(totalCourier, true)}"
                    )
                )
            }

            ReportType.PRODUCT_PROFIT -> {
                val profitRows = AccountingEngine.computeProductWiseProfit(products, orders, returns)
                val rows = profitRows.map { r ->
                    listOf(
                        r.sku,
                        r.productName,
                        r.unitsSold.toString(),
                        r.unitsReturned.toString(),
                        r.netUnits.toString(),
                        BdtFormatter.formatPlain(r.netRevenue),
                        BdtFormatter.formatPlain(r.productCost),
                        BdtFormatter.formatPlain(r.packagingCost),
                        BdtFormatter.formatPlain(r.grossProfit),
                        String.format(java.util.Locale.US, "%.1f%%", r.marginPercent)
                    )
                }
                ReportTable(
                    title = "Autonstyle Product-wise Profit Report",
                    subtitle = generatedAt,
                    headers = listOf("SKU", "Product", "Sold", "Returned", "Net Qty", "Net Revenue", "Prod Cost", "Pkg Cost", "Profit (BDT)", "Margin"),
                    rows = rows,
                    summaryLines = listOf(
                        "Products Analyzed: ${profitRows.size}",
                        "Total Product Gross Profit: ${BdtFormatter.format(profitRows.sumOf { it.grossProfit }, true)}"
                    )
                )
            }

            ReportType.MONTHLY_PNL -> {
                val byMonth = orders.groupBy { DhakaDateTime.monthYearLabel(it.orderEpochMs) }
                val returnMonths = returns.groupBy { DhakaDateTime.monthYearLabel(it.returnEpochMs) }
                val expenseMonths = expenses.groupBy { DhakaDateTime.monthYearLabel(it.expenseEpochMs) }
                val allMonths = (byMonth.keys + returnMonths.keys + expenseMonths.keys)
                    .distinct()
                    .sortedByDescending { it }
                    .ifEmpty { listOf(DhakaDateTime.monthYearLabel(System.currentTimeMillis())) }

                val rows = allMonths.map { monthKey ->
                    val mOrders = byMonth[monthKey].orEmpty()
                    val mReturns = returnMonths[monthKey].orEmpty()
                    val mExpenses = expenseMonths[monthKey].orEmpty()
                    val pnl = AccountingEngine.computeProfitAndLoss(
                        orders = mOrders,
                        returns = mReturns,
                        expenses = mExpenses,
                        packagingItems = packagingItems,
                        stockTransactions = emptyList(),
                        startEpochMs = 0L,
                        endEpochMs = Long.MAX_VALUE,
                        periodLabel = DhakaDateTime.monthDisplayName(monthKey)
                    )
                    listOf(
                        pnl.periodLabel,
                        pnl.activeOrderCount.toString(),
                        BdtFormatter.formatPlain(pnl.netSalesRevenue),
                        BdtFormatter.formatPlain(pnl.productCost),
                        BdtFormatter.formatPlain(pnl.packagingMaterialCost),
                        BdtFormatter.formatPlain(pnl.deliveryExpenses),
                        BdtFormatter.formatPlain(pnl.returnExpenses),
                        BdtFormatter.formatPlain(pnl.otherExpenses),
                        BdtFormatter.formatPlain(pnl.netProfit)
                    )
                }
                ReportTable(
                    title = "Autonstyle Monthly Profit & Loss",
                    subtitle = generatedAt,
                    headers = listOf("Month", "Orders", "Net Sales", "Product Cost", "Packaging Cost", "Delivery Exp", "Return Exp", "Other Exp", "Net Profit (BDT)"),
                    rows = rows,
                    summaryLines = listOf(
                        "Months Covered: ${rows.size}",
                        "Formula: Sales Revenue - Product Cost - Packaging Cost - Delivery - Return - Other Expenses"
                    )
                )
            }
        }
    }
}

object ReportExporter {

    fun exportToUri(
        context: Context,
        uri: Uri,
        table: ReportTable,
        format: ExportFormat
    ): Result<Unit> {
        return try {
            val bytes = when (format) {
                ExportFormat.CSV -> generateCsvBytes(table)
                ExportFormat.EXCEL -> generateXlsxBytes(table)
                ExportFormat.PDF -> generatePdfBytes(table)
            }
            context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                out.write(bytes)
                out.flush()
            } ?: return Result.failure(IllegalStateException("Could not open destination file."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateCsvBytes(table: ReportTable): ByteArray {
        val sb = StringBuilder()
        sb.appendLine(csvCell(table.title))
        sb.appendLine(csvCell(table.subtitle))
        sb.appendLine()
        sb.appendLine(table.headers.joinToString(",") { csvCell(it) })
        for (row in table.rows) {
            sb.appendLine(row.joinToString(",") { csvCell(it) })
        }
        sb.appendLine()
        for (line in table.summaryLines) {
            sb.appendLine(csvCell(line))
        }
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        return bom + sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun csvCell(raw: String): String {
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun generateXlsxBytes(table: ReportTable): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            writeZipEntry(
                zip,
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            )
            writeZipEntry(
                zip,
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )
            writeZipEntry(
                zip,
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Report" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            )
            writeZipEntry(
                zip,
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            )
            writeZipEntry(
                zip,
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0A1D37"/></patternFill></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""
            )
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheetXml(table))
        }
        return out.toByteArray()
    }

    private fun buildSheetXml(table: ReportTable): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        var rowIdx = 1
        sb.append(buildXmlRow(rowIdx++, listOf(table.title), styleIndex = 0))
        sb.append(buildXmlRow(rowIdx++, listOf(table.subtitle), styleIndex = 0))
        rowIdx++
        sb.append(buildXmlRow(rowIdx++, table.headers, styleIndex = 1))
        for (row in table.rows) {
            sb.append(buildXmlRow(rowIdx++, row, styleIndex = 0))
        }
        rowIdx++
        for (line in table.summaryLines) {
            sb.append(buildXmlRow(rowIdx++, listOf(line), styleIndex = 0))
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun buildXmlRow(rowIndex: Int, cells: List<String>, styleIndex: Int): String {
        val sb = StringBuilder("<row r=\"$rowIndex\">")
        cells.forEachIndexed { colIndex, value ->
            val colRef = columnLetter(colIndex) + rowIndex
            val styleAttr = if (styleIndex > 0) " s=\"$styleIndex\"" else ""
            val num = value.toDoubleOrNull()
            if (num != null && !value.startsWith("0") && value.length <= 12) {
                sb.append("<c r=\"$colRef\"$styleAttr><v>$value</v></c>")
            } else {
                sb.append("<c r=\"$colRef\"$styleAttr t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>")
            }
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun columnLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.append(('A'.code + (i % 26)).toChar())
            i = (i / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    fun generatePdfBytes(table: ReportTable): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 842 // A4 Landscape for wide financial tables
        val pageHeight = 595
        val margin = 32f

        val navyPaint = Paint().apply { color = Color.parseColor("#0A1D37") }
        val redPaint = Paint().apply { color = Color.parseColor("#D91E2A") }
        val limePaint = Paint().apply { color = Color.parseColor("#C8E600") }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 10f
            isAntiAlias = true
        }
        val thPaint = Paint().apply {
            color = Color.parseColor("#0A1D37")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tdPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
            isAntiAlias = true
        }
        val summaryPaint = Paint().apply {
            color = Color.parseColor("#0A1D37")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val usableWidth = pageWidth - margin * 2
        val colCount = table.headers.size.coerceAtLeast(1)
        val colWidth = usableWidth / colCount

        fun drawHeader(canvas: Canvas, pageNum: Int): Float {
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 66f, navyPaint)
            canvas.drawRect(0f, 62f, pageWidth * 0.7f, 66f, redPaint)
            canvas.drawRect(pageWidth * 0.7f, 62f, pageWidth.toFloat(), 66f, limePaint)
            canvas.drawText("AUTONSTYLE  |  ${table.title}", margin, 32f, titlePaint)
            canvas.drawText("${table.subtitle}   •   Page $pageNum", margin, 50f, subPaint)

            val tableTop = 82f
            canvas.drawRect(margin, tableTop, pageWidth - margin, tableTop + 22f, headerBgPaint)
            table.headers.forEachIndexed { idx, h ->
                val x = margin + idx * colWidth + 4f
                canvas.drawText(truncateText(h, thPaint, colWidth - 8f), x, tableTop + 15f, thPaint)
            }
            canvas.drawLine(margin, tableTop + 22f, pageWidth - margin, tableTop + 22f, dividerPaint)
            return tableTop + 26f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var y = drawHeader(page.canvas, pageNum)

        for (row in table.rows) {
            if (y > pageHeight - 60f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                y = drawHeader(page.canvas, pageNum)
            }
            row.forEachIndexed { idx, cell ->
                if (idx < colCount) {
                    val x = margin + idx * colWidth + 4f
                    page.canvas.drawText(truncateText(cell, tdPaint, colWidth - 8f), x, y + 12f, tdPaint)
                }
            }
            y += 18f
            page.canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 2f
        }

        y += 14f
        if (y > pageHeight - 40f) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            y = 48f
        }
        for (line in table.summaryLines) {
            page.canvas.drawText(line, margin, y, summaryPaint)
            y += 16f
        }
        doc.finishPage(page)

        val baos = ByteArrayOutputStream()
        doc.writeTo(baos)
        doc.close()
        return baos.toByteArray()
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return if (end <= 1) text.take(1) else text.substring(0, end) + "…"
    }
}
