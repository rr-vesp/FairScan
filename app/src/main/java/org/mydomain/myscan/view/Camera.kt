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
package org.mydomain.myscan.view

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mydomain.myscan.LiveAnalysisState
import org.mydomain.myscan.MainViewModel
import org.mydomain.myscan.ui.theme.MyScanTheme

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    liveAnalysisState: LiveAnalysisState,
    onImageAnalyzed: (ImageProxy) -> Unit,
    onFinalizePressed: () -> Unit,
    modifier: Modifier,
) {
    val showPageDialog = rememberSaveable { mutableStateOf(false) }
    val isProcessing = rememberSaveable { mutableStateOf(false) }
    val pageToValidate by viewModel.pageToValidate.collectAsStateWithLifecycle()

    val captureController = remember { CameraCaptureController() }
    DisposableEffect(Unit) {
        onDispose { captureController.shutdown() }
    }

    CameraScreenContent(
        modifier,
        cameraPreview = {
            CameraPreview(
                onImageAnalyzed = onImageAnalyzed,
                captureController = captureController
            ) },
        pageCount = viewModel.pageCount(),
        liveAnalysisState = if (showPageDialog.value) LiveAnalysisState() else liveAnalysisState,
        onCapture = {
            Log.i("MyScan", "Pressed <Capture>")
            viewModel.liveAnalysisEnabled = false
            showPageDialog.value = true
            isProcessing.value = true
            captureController.takePicture(
                onImageCaptured = { imageProxy ->
                    if (imageProxy != null) {
                        viewModel.processCapturedImageThen(imageProxy) {
                            isProcessing.value = false
                            viewModel.liveAnalysisEnabled = true
                            Log.i("MyScan", "Capture process finished")
                        }
                    } else {
                        Log.e("MyScan", "Error during image capture")
                        isProcessing.value = false
                        viewModel.liveAnalysisEnabled = true
                    }
                }
            )
        },
        onFinalizePressed = onFinalizePressed
    )

    if (showPageDialog.value) {
        PageValidationDialog(
            isProcessing = isProcessing.value,
            pageBitmap = pageToValidate,
            onConfirm = {
                pageToValidate?.let { viewModel.addPage(it) }
                showPageDialog.value = false
            },
            onReject = {
                showPageDialog.value = false
            },
            onDismiss = {
                showPageDialog.value = false
            }
        )
    }
}

@Composable
private fun CameraScreenContent(
    modifier: Modifier,
    cameraPreview: @Composable () -> Unit,
    pageCount: Int,
    liveAnalysisState: LiveAnalysisState,
    onCapture: () -> Unit,
    onFinalizePressed: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraPreviewWithOverlay(cameraPreview, liveAnalysisState)
        MessageBox(liveAnalysisState.inferenceTime)

        CaptureButton(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
        CameraScreenFooter(
            pageCount = pageCount,
            onFinalizePressed = onFinalizePressed,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CaptureButton(onClick: () -> Unit, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(
                    width = 4.dp,
                    color = color.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color = color, shape = CircleShape)
        )
    }
}

@Composable
private fun CameraPreviewWithOverlay(
    cameraPreview: @Composable () -> Unit,
    liveAnalysisState: LiveAnalysisState
) {
    val width = LocalConfiguration.current.screenWidthDp
    val height = width / 3 * 4
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
    ) {
        cameraPreview()
        AnalysisOverlay(liveAnalysisState)
    }
}

@Composable
fun MessageBox(inferenceTime: Long) {
    Text(
        text = if(inferenceTime == 0L) "" else "Segmentation time: $inferenceTime ms",
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        color = Color.Gray,
    )
}

@Composable
fun CameraScreenFooter(
    pageCount: Int,
    onFinalizePressed: () -> Unit,
    modifier: Modifier,
) {
    Surface (
        color = MaterialTheme.colorScheme.inverseOnSurface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        Row (
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pages : $pageCount",
                style = MaterialTheme.typography.bodyMedium
            )

            Button (
                onClick = onFinalizePressed,
                enabled = pageCount > 0
            ) {
                Text("Finish")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    MyScanTheme {
        CameraScreenContent(
            modifier = Modifier,
            cameraPreview = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera Preview", color = Color.White)
                }
            },
            pageCount = 3,
            liveAnalysisState = LiveAnalysisState(),
            onCapture = {},
            onFinalizePressed = {})
    }
}
