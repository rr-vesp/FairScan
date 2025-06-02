package org.mydomain.myscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val imageSegmentationService: ImageSegmentationService): ViewModel() {

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MainViewModel(ImageSegmentationService(context)) as T
            }
        }
    }

    private var _cameraScreenState = MutableStateFlow(CameraScreenState("just started"))
    val cameraScreenState: StateFlow<CameraScreenState> = _cameraScreenState.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Camera)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // TODO store images on disk
    private val _pages = MutableStateFlow<List<Bitmap>>(listOf())
    val pages: StateFlow<List<Bitmap>> = _pages

    init {
        viewModelScope.launch {
            imageSegmentationService.initialize()
            imageSegmentationService.segmentation
                .filterNotNull()
                .map {
                    val binaryMask = it.segmentation.toBinaryMask()
                    CameraScreenState(
                        detectionMessage = "Inference done",
                        inferenceTime = it.inferenceTime,
                        binaryMask = binaryMask,
                        documentQuad = detectDocumentQuad(binaryMask)
                    )
                }
                .collect {
                    _cameraScreenState.value = it
                }
        }
    }

    fun segment(imageProxy: ImageProxy) {
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

    fun processCapturedImageThen(imageProxy: ImageProxy, onResult: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            val processedImage = processCapturedImage(imageProxy)
            onResult(processedImage)
        }
    }

    private suspend fun processCapturedImage(imageProxy: ImageProxy): Bitmap? = withContext(Dispatchers.IO) {
        var corrected: Bitmap? = null
        val bitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees)
        val segmentation = imageSegmentationService.runSegmentationAndReturn(bitmap, 0)
        if (segmentation != null) {
            val mask = segmentation.segmentation.toBinaryMask()
            val quad = detectDocumentQuad(mask)
            if (quad != null) {
                val resizedQuad = quad.scaledTo(mask.width, mask.height, bitmap.width, bitmap.height)
                corrected = extractDocument(bitmap, resizedQuad)
            }
        }
        return@withContext corrected
    }

    fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    fun addPage(bitmap: Bitmap) {
        _pages.update { list -> list.plus(bitmap) }
    }

    fun pageCount(): Int = _pages.value.size
}
