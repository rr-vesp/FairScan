package org.mydomain.myscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max

fun createPdfFromBitmaps (bitmaps: List<Bitmap>): PdfDocument {
    val document = PdfDocument()
    for ((index, bitmap) in bitmaps.map { resizeImage(it) }.withIndex()) {
        val jpegStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 72, jpegStream)
        val compressedBytes = jpegStream.toByteArray()
        val compressedBitmap =
            BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
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
