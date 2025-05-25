package org.mydomain.myscan

import androidx.compose.runtime.Immutable

@Immutable
data class UiState(
    val detectionMessage: String? = null,
    val inferenceTime: Long = 0L,
    val errorMessage: String? = null,
)
