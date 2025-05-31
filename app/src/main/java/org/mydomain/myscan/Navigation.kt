package org.mydomain.myscan

import android.graphics.Bitmap

sealed class Screen {
    object Camera : Screen()
    data class PagePreview(
        val image: Bitmap? = null,
        val isProcessing: Boolean = true
    ) : Screen()
}
