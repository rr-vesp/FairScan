package org.mydomain.myscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.scale
import kotlin.math.max

fun createPdfFromJpegs (jpegs: Sequence<ByteArray>): PdfDocument {
    val document = PdfDocument()
    for ((index, jpegBytes) in jpegs.withIndex()) {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
    }
    return document
}

fun resizeImage(original: Bitmap): Bitmap {
    val targetMax = 1500
    if (max(original.width, original.height) < targetMax)
        return original;
    var targetWidth = targetMax
    var targetHeight = original.height * targetWidth / original.width
    if (original.width < original.height) {
        targetHeight = targetMax
        targetWidth = original.width * targetHeight / original.height
    }
    return original.scale(targetWidth, targetHeight)
}
