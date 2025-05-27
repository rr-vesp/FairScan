package org.mydomain.myscan

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(private val imageSegmentationService: ImageSegmentationService): ViewModel() {

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MainViewModel(ImageSegmentationService(context)) as T
            }
        }
    }

    private var _uiState = MutableStateFlow(UiState("just started"))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            imageSegmentationService.initialize()
            imageSegmentationService.segmentation
                .filterNotNull()
                .map {
                    UiState(
                        "Found ${numberOfObjectsDetected(it.segmentation)} objects!",
                        it.inferenceTime,
                        it.segmentation.toBitmap())
                }
                .collect {
                    Log.d("MyScan", "New UIstate ${it}")
                    _uiState.value = it
                }
        }
    }

    fun numberOfObjectsDetected(segmentation: ImageSegmentationService.Segmentation) : Int {
        val tensor = segmentation.mask;
        val buffer = tensor.buffer
        val uniqueValues = HashSet<Int>()
        for (i in 0..tensor.width * tensor.height - 1) {
            uniqueValues.add(buffer[i].toInt())
        }
        return uniqueValues.size - 1;
    }

    fun segment(imageProxy: ImageProxy) {
        Log.d("MyScan", "MainViewModel.Calling segment")
        viewModelScope.launch {
            imageSegmentationService.runSegmentation(
                imageProxy.toBitmap(),
                imageProxy.imageInfo.rotationDegrees,
            )
            imageProxy.close()
        }
    }

}