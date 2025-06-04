package org.mydomain.myscan

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.OutputStream
import kotlin.math.max

fun writePdfFromJpegs(jpegs: Sequence<ByteArray>, outputStream: OutputStream) {
    PDDocument().use { document ->
        for (jpegBytes in jpegs) {
            val image = JPEGFactory.createFromByteArray(document, jpegBytes)
            val page = PDPage(PDRectangle(image.width.toFloat(), image.height.toFloat()))
            document.addPage(page)
            val contentStream = PDPageContentStream(document, page, AppendMode.OVERWRITE, false)
            contentStream.drawImage(image, 0f, 0f)
            contentStream.close()
        }
        document.save(outputStream)
    }
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
