package org.mydomain.myscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Color.argb
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ColorSpaceType
import org.tensorflow.lite.support.image.ImageProperties
import org.tensorflow.lite.support.image.TensorImage
import java.nio.ByteBuffer
import java.nio.FloatBuffer

// TODO Review and remove unneeded code
class ImageSegmentationService(private val context: Context) {

    companion object {
        private const val TAG = "ImageSegmentation"
    }

    private val _segmentation = MutableStateFlow<SegmentationResult?>(null)
    val segmentation: StateFlow<SegmentationResult?> = _segmentation.asStateFlow()

    val error: SharedFlow<Throwable?>
        get() = _error
    private val _error = MutableSharedFlow<Throwable?>()

    private var interpreter: Interpreter? = null

    suspend fun initialize() {
        interpreter = try {
            val litertBuffer = FileUtil.loadMappedFile(context, "mydeeplabv3.tflite")
            Log.i(TAG, "Loaded LiteRT model")
            val options = Interpreter.Options().apply {
                numThreads = 2
            }
            Interpreter(litertBuffer, options)
        } catch (e: Exception) {
            Log.i(TAG, "Failed to load LiteRT model: ${e.message}")
            _error.emit(e)
            null
        }
    }

    suspend fun runSegmentation(bitmap: Bitmap, rotationDegrees: Int) {
        try {
            withContext(Dispatchers.IO) {
                if (interpreter == null) return@withContext
                val startTime = SystemClock.uptimeMillis()

                val (_, _, h, w) = interpreter?.getInputTensor(0)?.shape() ?: return@withContext
                val dataType = interpreter?.getInputTensor(0)?.dataType()
                Log.i(TAG, "segment, input shape: ${interpreter!!.getInputTensor(0).shape().asList()} data type=${dataType}")

                // Preprocess manually into CHW float buffer
                val inputBuffer = bitmapToCHWFloatBuffer(bitmap, width = w, height = h, rotationDegrees)

                val (_, cOut, hOut, wOut) = interpreter!!.getOutputTensor(0).shape()
                val outputBuffer = FloatBuffer.allocate(cOut * hOut * wOut)

                // Run inference
                outputBuffer.rewind()
                interpreter?.run(inputBuffer, outputBuffer)

                val inferenceTime = SystemClock.uptimeMillis() - startTime
                if (isActive) {
                    val segmentResult = processOutputBuffer(outputBuffer,  wOut, hOut, cOut)
                    _segmentation.value = SegmentationResult(segmentResult, inferenceTime)
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "Image segmentation error occurred: ${e.message}")
            _error.emit(e)
        }
    }

    fun bitmapToCHWFloatBuffer(bitmap: Bitmap, width: Int, height: Int, rotationDegrees: Int): FloatBuffer {
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val resized = rotatedBitmap.scale(width, height)
        val buffer = FloatBuffer.allocate(1 * 3 * height * width)
        buffer.rewind()

        val mean = floatArrayOf(0.4611f, 0.4359f, 0.3905f)
        val std = floatArrayOf(0.2193f, 0.2150f, 0.2109f)

        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        // Fill buffer in CHW order
        for (c in 0..2) {
            for (i in 0 until height) {
                for (j in 0 until width) {
                    val pixel = pixels[i * width + j]
                    val value = when (c) {
                        0 -> (pixel shr 16 and 0xFF) // R
                        1 -> (pixel shr 8 and 0xFF)  // G
                        2 -> (pixel and 0xFF)        // B
                        else -> 0
                    }
                    val normalized = (value / 255f - mean[c]) / std[c]
                    buffer.put(normalized)
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun processOutputBuffer(outputBuffer: FloatBuffer, w: Int, h: Int, c: Int): Segmentation {
        outputBuffer.rewind()
        val inferenceData =
            InferenceData(width = w, height = h, channels = c, buffer = outputBuffer)
        val mask = processImage(inferenceData)

        val imageProperties =
            ImageProperties
                .builder()
                .setWidth(inferenceData.width)
                .setHeight(inferenceData.height)
                .setColorSpaceType(ColorSpaceType.GRAYSCALE)
                .build()
        val maskImage = TensorImage()
        maskImage.load(mask, imageProperties)
        return Segmentation(maskImage)
    }

    private fun processImage(inferenceData: InferenceData): ByteBuffer {
        val mask = ByteBuffer.allocateDirect(inferenceData.width * inferenceData.height)
        for (i in 0 until inferenceData.height) {
            for (j in 0 until inferenceData.width) {
                var maxIndex = 0
                var maxValue = inferenceData.buffer.get(i * inferenceData.width + j)

                for (c in 1 until inferenceData.channels) {
                    val value = inferenceData.buffer.get(
                        c * inferenceData.height * inferenceData.width + i * inferenceData.width + j)
                    if (value > maxValue) {
                        maxValue = value
                        maxIndex = c
                    }
                }

                mask.put(i * inferenceData.width + j, maxIndex.toByte())
            }
        }

        return mask
    }

    data class Segmentation(val mask: TensorImage) {
        fun toBitmap(): Bitmap {
            val width = mask.width
            val height = mask.height
            val pixels = IntArray(width * height)
            val green = argb(128, 0, 255, 0)
            for (i in 0 until height) {
                for (j in 0 until width) {
                    val index = i * width + j
                    val classId = mask.buffer[index].toInt() and 0xFF // Unsigned byte
                    pixels[index] = if (classId == 0) 0 else green
                }
            }
            return createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }


    data class SegmentationResult(
        val segmentation: Segmentation,
        val inferenceTime: Long
    )

    data class InferenceData(
        val width: Int,
        val height: Int,
        val channels: Int,
        val buffer: FloatBuffer,
    )
}
