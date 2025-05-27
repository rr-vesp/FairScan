package org.mydomain.myscan

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class UiState(
    val detectionMessage: String? = null,
    val inferenceTime: Long = 0L,
    val overlayBitmap: Bitmap? = null,
    val errorMessage: String? = null,
)
