/*
 * Copyright 2025 Pierre-Yves Nicolas
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.mydomain.myscan

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

fun enhanceCapturedImage(img: Mat): Mat {
    return if (isColoredDocument(img)) {
        Log.i("PostProcessing", "color document")
        val result = Mat()
        Core.convertScaleAbs(img, result, 1.2, 10.0)
        result
    } else {
        Log.i("PostProcessing", "grayscale document")
        val gray = correctLighting(img)
        val contrastedGray = enhanceContrastGray(gray)
        val result = Mat()
        Imgproc.cvtColor(contrastedGray, result, Imgproc.COLOR_GRAY2BGR)
        result
    }
}

fun isColoredDocument(img: Mat, threshold: Double = 4.0): Boolean {
    val lab = Mat()
    Imgproc.cvtColor(img, lab, Imgproc.COLOR_BGR2Lab)
    val channels = ArrayList<Mat>()
    Core.split(lab, channels)

    val aStd = MatOfDouble()
    val bStd = MatOfDouble()
    Core.meanStdDev(channels[1], MatOfDouble(), aStd)
    Core.meanStdDev(channels[2], MatOfDouble(), bStd)

    val result = (aStd.toArray()[0] + bStd.toArray()[0]) / 2.0
    return result > threshold
}

// TODO the radius should depend on the image size
fun correctLighting(img: Mat, radius: Int = 100): Mat {
    val gray = Mat()
    Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY)

    val blur = Mat()
    val kernelSize = 2 * radius + 1
    Imgproc.GaussianBlur(gray, blur, Size(kernelSize.toDouble(), kernelSize.toDouble()), 0.0)

    val normalized = Mat()
    Core.divide(gray, blur, normalized, 255.0)
    return normalized
}

fun enhanceContrastGray(img: Mat): Mat {
    val flat = img.reshape(0, 1)
    val sorted = Mat()
    Core.sort(flat, sorted, Core.SORT_ASCENDING)

    val totalPixels = sorted.cols()
    val pLow = sorted[0, (totalPixels * 0.01).toInt()][0]
    val pHigh = sorted[0, (totalPixels * 0.95).toInt()][0]

    val result = Mat(img.size(), img.type())
    img.convertTo(result, CvType.CV_32F)
    Core.subtract(result, Scalar(pLow), result)
    Core.multiply(result, Scalar(255.0 * 1.03 / (pHigh - pLow)), result)
    Core.min(result, Scalar(255.0), result)
    Core.max(result, Scalar(0.0), result)
    result.convertTo(result, CvType.CV_8U)
    return result
}
