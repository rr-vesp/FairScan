package org.mydomain.myscan

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun createPdfFromBitmaps(bitmaps: List<Bitmap>, outputFile: File): Boolean {
    val document = PdfDocument()
    try {
        for ((index, bitmap) in bitmaps.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }
        FileOutputStream(outputFile).use { outputStream ->
            document.writeTo(outputStream)
        }
        return true
    } catch (e: IOException) {
        Log.e("MyScan", "Error writing PDF: ${e.message}")
        return false
    } finally {
        document.close()
    }
}