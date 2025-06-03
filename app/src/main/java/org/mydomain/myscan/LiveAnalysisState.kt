package org.mydomain.myscan

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class LiveAnalysisState(
    val inferenceTime: Long = 0L,
    val binaryMask: Bitmap? = null,
    val documentQuad: Quad? = null,
)
