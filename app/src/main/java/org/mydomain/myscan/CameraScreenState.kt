package org.mydomain.myscan

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class CameraScreenState(
    val detectionMessage: String? = null,
    val inferenceTime: Long = 0L,
    val binaryMask: Bitmap? = null,
    val errorMessage: String? = null,
    val documentQuad: Quad? = null,
)
