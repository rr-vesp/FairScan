package org.mydomain.myscan.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun PagePreviewScreen(
    image: Bitmap?,
    isProcessing: Boolean,
    onBackPressed: () -> Unit,
    onSavePressed: (Bitmap) -> Unit,
    onSharePressed: (Bitmap) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isProcessing -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
            image != null -> {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Document preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Button (onClick = { onSavePressed(image) }) {
                        Text("Save PDF")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button (onClick = { onSharePressed(image) }) {
                        Text("Share PDF")
                    }
                }
            }
            else -> {
                Text(
                    text = "No image is available.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        IconButton (
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}
