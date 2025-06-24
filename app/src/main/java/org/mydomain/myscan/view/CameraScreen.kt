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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.mydomain.myscan.LiveAnalysisState
import org.mydomain.myscan.MainViewModel
import org.mydomain.myscan.MainViewModel.CaptureState
import org.mydomain.myscan.Screen
import org.mydomain.myscan.ui.theme.MyScanTheme

data class CameraUiState(
    val pageCount: Int,
    val liveAnalysisState: LiveAnalysisState,
    val captureState: CaptureState
)

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    liveAnalysisState: LiveAnalysisState,
    onImageAnalyzed: (ImageProxy) -> Unit,
    onFinalizePressed: () -> Unit,
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val pageIds by viewModel.pageIds.collectAsStateWithLifecycle()

    val captureController = remember { CameraCaptureController() }
    DisposableEffect(Unit) {
        onDispose { captureController.shutdown() }
    }

    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    if (captureState.isProcessed()) {
        LaunchedEffect(captureState) {
            delay(1500)
            viewModel.addProcessedImage()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(pageIds.size) {
        if (pageIds.isNotEmpty()) {
            listState.animateScrollToItem(pageIds.lastIndex)
        }
    }
    CameraScreenScaffold(
        cameraPreview = {
            CameraPreview(
                onImageAnalyzed = onImageAnalyzed,
                captureController = captureController,
                onPreviewViewReady = { view -> previewView = view }
            )
        },
        pageList = {
            CommonPageList(
                pageIds = pageIds,
                imageLoader = { id -> viewModel.getBitmap(id) },
                onPageClick = { index -> viewModel.navigateTo(Screen.FinalizeDocument(index)) },
                listState = listState
            )
        },
        cameraUiState = CameraUiState(pageIds.size, liveAnalysisState, captureState),
        onCapture = {
            Log.i("MyScan", "Pressed <Capture>")
            viewModel.onCapturePressed(previewView?.bitmap)
            captureController.takePicture(
                onImageCaptured = { imageProxy -> viewModel.onImageCaptured(imageProxy) }
            )
        },
        onFinalizePressed = onFinalizePressed,
    )
}

@Composable
private fun CameraScreenScaffold(
    cameraPreview: @Composable () -> Unit,
    pageList: @Composable () -> Unit,
    cameraUiState: CameraUiState,
    onCapture: () -> Unit,
    onFinalizePressed: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            CameraScreenFooter(
                pageList = pageList,
                pageCount = cameraUiState.pageCount,
                onFinalizePressed = onFinalizePressed,
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()).fillMaxSize()) {
            CameraPreviewWithOverlay(cameraPreview, cameraUiState)
            MessageBox(cameraUiState.liveAnalysisState.inferenceTime)
            CaptureButton(
                onClick = onCapture,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
            cameraUiState.captureState.processedImage?.let {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize()
                )
                {}
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                )
            }
        }
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
    cameraUiState: CameraUiState
) {
    val width = LocalConfiguration.current.screenWidthDp
    val height = width / 3 * 4
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
    ) {
        cameraPreview()
        AnalysisOverlay(cameraUiState.liveAnalysisState)
        cameraUiState.captureState.frozenImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
            )

        }
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
    pageList:  @Composable () -> Unit,
    pageCount: Int,
    onFinalizePressed: () -> Unit,
) {
    Column (modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
        pageList()
        BottomAppBar(
            tonalElevation = 4.dp,
        ) {
            Row (
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 1.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$pageCount pages",
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
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    ScreenPreview(CaptureState())
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreviewWithProcessedImage() {
    ScreenPreview(CaptureState(processedImage = debugImage("gallica.bnf.fr-bpt6k5530456s-1.jpg")))
}

@Composable
private fun ScreenPreview(captureState: CaptureState) {
    val context = LocalContext.current
    MyScanTheme {
        CameraScreenScaffold(
            cameraPreview = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(
                        debugImage("uncropped/img01.jpg").asImageBitmap(),
                        contentDescription = null
                    )
                }
            },
            pageList = {
                CommonPageList(
                    pageIds = listOf(1, 2, 2, 2).map { "gallica.bnf.fr-bpt6k5530456s-$it.jpg" },
                    imageLoader = { id ->
                        context.assets.open(id).use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    },
                    onPageClick = {},
                    listState = LazyListState()
                )
            },
            cameraUiState = CameraUiState(pageCount = 4, LiveAnalysisState(), captureState),
            onCapture = {},
            onFinalizePressed = {},
        )
    }
}

@Composable
private fun debugImage(imgName: String): Bitmap {
    val context = LocalContext.current
    return context.assets.open(imgName).use { input ->
        BitmapFactory.decodeStream(input)
    }
}
