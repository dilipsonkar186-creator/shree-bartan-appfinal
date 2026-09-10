package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtils {

    fun generateAndSharePdf(
        context: Context,
        customer: CustomerEntity?,
        transactions: List<TransactionEntity>,
        totalGoods: Double,
        totalPaid: Double,
        dues: Double
    ) {
        val customerName = customer?.name ?: "Customer"
        val customerPhone = customer?.phone ?: "N/A"
        val customerPageNo = customer?.pageNumber ?: ""
        val customerAddress = customer?.address ?: ""

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points (72 dpi)
            val pageHeight = 842 // A4 height in points

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val currentDateStr = dateFormat.format(Date())

            var y = 0

            // HEADER BANNER
            paint.color = Color.rgb(26, 35, 126) // Deep Navy Blue
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText("SHREE BARTAN STORE", 30f, 42f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("CUSTOMER LEDGER STATEMENT", 30f, 65f, paint)

            paint.textSize = 10f
            val dateText = "Date: $currentDateStr"
            val dateWidth = paint.measureText(dateText)
            canvas.drawText(dateText, pageWidth - 30f - dateWidth, 65f, paint)

            y = 110

            // CUSTOMER DETAILS BOX
            paint.color = Color.rgb(245, 247, 250) // Light grey/blue background
            canvas.drawRoundRect(30f, y.toFloat(), (pageWidth - 30).toFloat(), (y + 70).toFloat(), 8f, 8f, paint)

            paint.color = Color.rgb(33, 33, 33)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("Customer Details:", 42f, (y + 22).toFloat(), paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11f
            val nameDisplay = if (customerPageNo.isNotBlank()) "Name: $customerName (Pg: $customerPageNo)" else "Name: $customerName"
            canvas.drawText(nameDisplay, 42f, (y + 42).toFloat(), paint)
            canvas.drawText("Phone: $customerPhone", 42f, (y + 58).toFloat(), paint)

            if (customerAddress.isNotBlank()) {
                var addrText = "Address: $customerAddress"
                if (addrText.length > 35) addrText = addrText.substring(0, 33) + ".."
                canvas.drawText(addrText, 280f, (y + 42).toFloat(), paint)
            }

            y += 85

            // FINANCIAL SUMMARY BOX
            paint.color = Color.rgb(238, 242, 246)
            canvas.drawRoundRect(30f, y.toFloat(), (pageWidth - 30).toFloat(), (y + 58).toFloat(), 8f, 8f, paint)

            val boxWidth = (pageWidth - 60) / 3

            // Column 1: Total Goods
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("TOTAL GOODS (GAVE)", 42f, (y + 20).toFloat(), paint)
            paint.color = Color.rgb(198, 40, 40) // Red
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Rs. ${String.format("%,.2f", totalGoods)}", 42f, (y + 42).toFloat(), paint)

            // Column 2: Total Paid
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("TOTAL PAID (GOT)", (30 + boxWidth + 12).toFloat(), (y + 20).toFloat(), paint)
            paint.color = Color.rgb(46, 125, 50) // Green
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Rs. ${String.format("%,.2f", totalPaid)}", (30 + boxWidth + 12).toFloat(), (y + 42).toFloat(), paint)

            // Column 3: Net Balance
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("NET BALANCE / DUES", (30 + boxWidth * 2 + 12).toFloat(), (y + 20).toFloat(), paint)
            paint.color = if (dues > 0) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Rs. ${String.format("%,.2f", dues)}", (30 + boxWidth * 2 + 12).toFloat(), (y + 42).toFloat(), paint)

            y += 75

            // TRANSACTION TABLE HEADER
            paint.color = Color.rgb(26, 35, 126)
            canvas.drawRect(30f, y.toFloat(), (pageWidth - 30).toFloat(), (y + 24).toFloat(), paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f

            canvas.drawText("DATE", 35f, (y + 16).toFloat(), paint)
            canvas.drawText("TYPE", 102f, (y + 16).toFloat(), paint)
            canvas.drawText("ITEM DESCRIPTION", 160f, (y + 16).toFloat(), paint)
            canvas.drawText("QTY / RATE", 330f, (y + 16).toFloat(), paint)
            canvas.drawText("AMOUNT", 415f, (y + 16).toFloat(), paint)
            canvas.drawText("BALANCE", 495f, (y + 16).toFloat(), paint)

            y += 24

            // Pre-calculate running balances chronologically
            val chronological = transactions.sortedWith(
                compareBy<TransactionEntity> { it.dateMillis }.thenBy { it.id }
            )
            val runningBalances = mutableMapOf<Long, Double>()
            var curBal = 0.0
            for (tx in chronological) {
                when (tx.type) {
                    "GOODS_PROVIDED" -> curBal += tx.totalAmount
                    "PAYMENT_DEPOSIT" -> curBal -= tx.totalAmount
                    "GOODS_RETURNED" -> {
                        // Returned item does not change balance
                    }
                }
                runningBalances[tx.id] = curBal
            }

            // TABLE ROWS
            val sortedTxs = transactions.sortedWith(
                compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.id }
            )

            for ((index, tx) in sortedTxs.withIndex()) {
                // Check page overflow
                if (y > pageHeight - 65) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    y = 40
                    paint.color = Color.rgb(26, 35, 126)
                    canvas.drawRect(30f, y.toFloat(), (pageWidth - 30).toFloat(), (y + 24).toFloat(), paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 9.5f

                    canvas.drawText("DATE", 35f, (y + 16).toFloat(), paint)
                    canvas.drawText("TYPE", 102f, (y + 16).toFloat(), paint)
                    canvas.drawText("ITEM DESCRIPTION", 160f, (y + 16).toFloat(), paint)
                    canvas.drawText("QTY / RATE", 330f, (y + 16).toFloat(), paint)
                    canvas.drawText("AMOUNT", 415f, (y + 16).toFloat(), paint)
                    canvas.drawText("BALANCE", 495f, (y + 16).toFloat(), paint)

                    y += 24
                }

                val isGoods = tx.type == "GOODS_PROVIDED"
                val isReturn = tx.type == "GOODS_RETURNED"

                // Row background (alternating)
                if (isReturn) {
                    paint.color = Color.rgb(255, 243, 224) // Light Orange for returned item
                } else if (index % 2 == 0) {
                    paint.color = Color.rgb(250, 250, 250)
                } else {
                    paint.color = Color.WHITE
                }
                canvas.drawRect(30f, y.toFloat(), (pageWidth - 30).toFloat(), (y + 22).toFloat(), paint)

                // Divider line
                paint.color = Color.rgb(224, 224, 224)
                paint.strokeWidth = 0.5f
                canvas.drawLine(30f, (y + 22).toFloat(), (pageWidth - 30).toFloat(), (y + 22).toFloat(), paint)

                // Date
                val txDate = dateOnlyFormat.format(Date(tx.dateMillis))
                paint.color = Color.rgb(66, 66, 66)
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(txDate, 35f, (y + 15).toFloat(), paint)

                // Type Tag
                when {
                    isGoods -> {
                        paint.color = Color.rgb(198, 40, 40)
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("GAVE", 102f, (y + 15).toFloat(), paint)
                    }
                    isReturn -> {
                        paint.color = Color.rgb(230, 81, 0)
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("RETURN ❌", 102f, (y + 15).toFloat(), paint)
                    }
                    else -> {
                        paint.color = Color.rgb(46, 125, 50)
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("GOT", 102f, (y + 15).toFloat(), paint)
                    }
                }

                // Description
                paint.color = if (isReturn) Color.rgb(230, 81, 0) else Color.rgb(33, 33, 33)
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (isReturn) Typeface.BOLD else Typeface.NORMAL)
                paint.isStrikeThruText = isReturn

                var desc = tx.itemDescription
                if (desc.length > 25) desc = desc.substring(0, 23) + ".."
                canvas.drawText(desc, 160f, (y + 15).toFloat(), paint)
                paint.isStrikeThruText = false

                // Qty / Rate
                paint.color = Color.rgb(97, 97, 97)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val qtyStr = if (isGoods || isReturn) {
                    val q = if (tx.quantityDouble > 0) String.format("%.1f", tx.quantityDouble).removeSuffix(".0") else tx.quantity.toString()
                    "$q ${tx.unitType} @ Rs.${String.format("%.0f", tx.unitPrice)}"
                } else {
                    "—"
                }
                canvas.drawText(qtyStr, 330f, (y + 15).toFloat(), paint)

                // Amount
                paint.color = when {
                    isGoods -> Color.rgb(198, 40, 40)
                    isReturn -> Color.rgb(230, 81, 0)
                    else -> Color.rgb(46, 125, 50)
                }
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.isStrikeThruText = isReturn
                val amtStr = (if (isGoods) "+Rs." else "-Rs.") + String.format("%.2f", tx.totalAmount)
                canvas.drawText(amtStr, 415f, (y + 15).toFloat(), paint)
                paint.isStrikeThruText = false

                // Running Balance Column
                val rowBal = runningBalances[tx.id] ?: 0.0
                paint.color = if (rowBal > 0) Color.rgb(198, 40, 40) else if (rowBal < 0) Color.rgb(46, 125, 50) else Color.rgb(100, 116, 139)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val balStr = "Rs." + String.format("%.2f", rowBal)
                canvas.drawText(balStr, 495f, (y + 15).toFloat(), paint)

                y += 22
            }

            // FOOTER STATEMENT
            if (y > pageHeight - 50) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40
            }

            y += 20
            paint.color = Color.rgb(158, 158, 158)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Thank you for your business! — Shree Bartan Store", 30f, y.toFloat(), paint)
            canvas.drawText("This is a computer-generated ledger statement.", 30f, (y + 14).toFloat(), paint)

            pdfDocument.finishPage(page)

            // Write PDF file
            val sanitizedName = customerName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "Statement_${sanitizedName}_${System.currentTimeMillis()}.pdf"
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val pdfFile = File(exportDir, fileName)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // FileProvider Content URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            // Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ledger Statement - $customerName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the PDF Ledger Statement for $customerName.\nNet Balance / Dues: Rs. ${String.format("%.2f", dues)}\nThank you!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Statement (PDF)")
            context.startActivity(chooser)

            Toast.makeText(context, "PDF Statement generated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
