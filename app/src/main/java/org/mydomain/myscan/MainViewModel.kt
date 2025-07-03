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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainViewModel(
    private val imageSegmentationService: ImageSegmentationService,
    private val imageRepository: ImageRepository,
    private val pdfDir: File,
): ViewModel() {

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MainViewModel(
                    ImageSegmentationService(context),
                    ImageRepository(context.filesDir),
                    File(context.cacheDir, "pdfs"),
                ) as T
            }
        }
    }

    private var _liveAnalysisState = MutableStateFlow(LiveAnalysisState())
    val liveAnalysisState: StateFlow<LiveAnalysisState> = _liveAnalysisState.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Camera)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _pageIds = MutableStateFlow<List<String>>(imageRepository.imageIds())
    val pageIds: StateFlow<List<String>> = _pageIds

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState

    init {
        viewModelScope.launch {
            imageSegmentationService.initialize()
            imageSegmentationService.segmentation
                .filterNotNull()
                .map {
                    val binaryMask = it.segmentation.toBinaryMask()
                    LiveAnalysisState(
                        inferenceTime = it.inferenceTime,
                        binaryMask = binaryMask,
                        documentQuad = detectDocumentQuad(binaryMask)
                    )
                }
                .collect {
                    _liveAnalysisState.value = it
                }
        }
    }

    sealed class CaptureState {
        open val frozenImage: Bitmap? = null

        object Idle : CaptureState()
        data class Capturing(override val frozenImage: Bitmap) : CaptureState()
        data class CaptureError(override val frozenImage: Bitmap) : CaptureState()
        data class CapturePreview(
            override val frozenImage: Bitmap,
            val processed: Bitmap
        ) : CaptureState()
    }


    fun onCapturePressed(frozenImage: Bitmap) {
        _captureState.value = CaptureState.Capturing(frozenImage)
    }

    private fun onCaptureProcessed(captured: Bitmap?) {
        val current = _captureState.value
        _captureState.value = when {
            current is CaptureState.Capturing && captured != null ->
                CaptureState.CapturePreview(current.frozenImage, captured)
            current is CaptureState.Capturing ->
                CaptureState.CaptureError(current.frozenImage)
            else -> CaptureState.Idle
        }
    }

    fun liveAnalysis(imageProxy: ImageProxy) {
        if (_captureState.value !is CaptureState.Idle) {
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            imageSegmentationService.runSegmentationAndEmit(
                imageProxy.toBitmap(),
                imageProxy.imageInfo.rotationDegrees,
            )
            imageProxy.close()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun onImageCaptured(imageProxy: ImageProxy?) {
        if (imageProxy != null) {
            viewModelScope.launch {
                val image = processCapturedImage(imageProxy)
                imageProxy.close()
                onCaptureProcessed(image)
            }
        } else {
            onCaptureProcessed(null)
        }
    }

    private suspend fun processCapturedImage(imageProxy: ImageProxy): Bitmap? = withContext(Dispatchers.IO) {
        var corrected: Bitmap? = null
        val bitmap = imageProxy.toBitmap()
        val segmentation = imageSegmentationService.runSegmentationAndReturn(bitmap, 0)
        if (segmentation != null) {
            val mask = segmentation.segmentation.toBinaryMask()
            val quad = detectDocumentQuad(mask)
            if (quad != null) {
                val resizedQuad = quad.scaledTo(mask.width, mask.height, bitmap.width, bitmap.height)
                corrected = extractDocument(bitmap, resizedQuad, imageProxy.imageInfo.rotationDegrees)
            }
        }
        return@withContext corrected
    }

    fun addProcessedImage(quality: Int = 75) {
        val current = _captureState.value
        if (current is CaptureState.CapturePreview) {
            val outputStream = ByteArrayOutputStream()
            current.processed.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val jpegBytes = outputStream.toByteArray()
            imageRepository.add(jpegBytes)
            _pageIds.value = imageRepository.imageIds()
        }
        _captureState.value = CaptureState.Idle
    }

    fun afterCaptureError() {
        _captureState.value = CaptureState.Idle
    }

    fun deletePage(id: String) {
        imageRepository.delete(id)
        _pageIds.value = imageRepository.imageIds()
    }

    fun startNewDocument() {
        _pageIds.value = listOf()
        viewModelScope.launch {
            imageRepository.clear()
        }
    }

    fun getBitmap(id: String): Bitmap? {
        val bytes = imageRepository.getContent(id)
        return bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    fun createPdf(outputStream: OutputStream) {
        val jpegs = imageRepository.imageIds().asSequence()
            .map { id -> imageRepository.getContent(id) }
            .filterNotNull()
        writePdfFromJpegs(jpegs, outputStream)
    }

    suspend fun generatePdf(): GeneratedPdf = withContext(Dispatchers.IO) {
        val pageCount = imageRepository.imageIds().size
        val file = File(pdfDir,"${System.currentTimeMillis()}.pdf")
        createPdf(FileOutputStream(file))
        val sizeBytes = file.length()
        val uri = file.toUri()
        return@withContext GeneratedPdf(uri, sizeBytes, pageCount)
    }
}

data class GeneratedPdf(
    val uri: Uri,
    val sizeInBytes: Long,
    val pageCount: Int,
)

data class PdfGenerationActions(
    val generatePdf: suspend () -> GeneratedPdf?,
    val onShare: (Uri) -> Unit,
    val onSave: (Uri) -> Unit,
    val onOpen: (Uri) -> Unit
)
