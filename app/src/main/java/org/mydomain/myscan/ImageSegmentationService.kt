package org.mydomain.myscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ColorSpaceType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ImageProperties
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.Random

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
            val litertBuffer = FileUtil.loadMappedFile(context, "deeplab_v3.tflite")
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

                val rotation = -rotationDegrees / 90
                val (_, h, w, _) = interpreter?.getOutputTensor(0)?.shape() ?: return@withContext
                val imageProcessor =
                    ImageProcessor
                        .Builder()
                        .add(ResizeOp(h, w, ResizeOp.ResizeMethod.BILINEAR))
                        .add(Rot90Op(rotation))
                        .add(NormalizeOp(127.5f, 127.5f))
                        .build()

                // Preprocess the image and convert it into a TensorImage for segmentation.
                val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
                val segmentResult = segment(tensorImage)
                val inferenceTime = SystemClock.uptimeMillis() - startTime
                if (isActive) {
                    _segmentation.value = SegmentationResult(segmentResult, inferenceTime)
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "Image segmentation error occurred: ${e.message}")
            _error.emit(e)
        }
    }


    private fun segment(tensorImage: TensorImage): Segmentation {
        val (_, h, w, c) = interpreter!!.getOutputTensor(0).shape()
        val outputBuffer = FloatBuffer.allocate(h * w * c)

        outputBuffer.rewind()
        interpreter?.run(tensorImage.tensorBuffer.buffer, outputBuffer)

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
        return Segmentation(listOf(maskImage))
    }

    private fun processImage(inferenceData: InferenceData): ByteBuffer {
        val mask = ByteBuffer.allocateDirect(inferenceData.width * inferenceData.height)
        for (i in 0 until inferenceData.height) {
            for (j in 0 until inferenceData.width) {
                val offset = inferenceData.channels * (i * inferenceData.width + j)

                var maxIndex = 0
                var maxValue = inferenceData.buffer.get(offset)

                for (index in 1 until inferenceData.channels) {
                    if (inferenceData.buffer.get(offset + index) > maxValue) {
                        maxValue = inferenceData.buffer.get(offset + index)
                        maxIndex = index
                    }
                }

                mask.put(i * inferenceData.width + j, maxIndex.toByte())
            }
        }

        return mask
    }

    data class Segmentation(
        val masks: List<TensorImage>
    )

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