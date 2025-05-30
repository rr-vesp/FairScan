package org.mydomain.myscan

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

fun detectDocumentQuad(mask: Bitmap): Quad? {
    val mat = Mat()
    Utils.bitmapToMat(mask, mat)

    val gray = Mat()
    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

    val blurred = Mat()
    Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

    val edges = Mat()
    Imgproc.Canny(blurred, edges, 75.0, 200.0)

    val contours = mutableListOf<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

    var biggest: MatOfPoint2f? = null
    var maxArea = 0.0

    for (contour in contours) {
        val contour2f = MatOfPoint2f(*contour.toArray())
        val peri = Imgproc.arcLength(contour2f, true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

        if (approx.total() == 4L) {
            val area = abs(Imgproc.contourArea(approx))
            if (area > maxArea) {
                maxArea = area
                biggest = approx
            }
        }
    }

    val vertices = biggest?.toList()?.map { Point(it.x.toInt(), it.y.toInt()) }
    return createQuad(vertices)
}
