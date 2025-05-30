package org.mydomain.myscan.view

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import org.mydomain.myscan.UiState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.graphics.scale
import org.mydomain.myscan.Point
import org.mydomain.myscan.scaledTo

@Composable
fun CameraScreen(
    uiState: UiState,
    onImageAnalyzed: (ImageProxy) -> Unit,
) {
    // TODO Check the errors in the logs before the user gives the required authorization
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Camera permission was denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val camera = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, camera) != PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(camera)
        }
    }

    val width = LocalConfiguration.current.screenWidthDp
    val height = width / 3 * 4
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
    ) {
        CameraPreview(onImageAnalyzed = onImageAnalyzed)
        AnalysisOverlay(uiState)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageAnalyzed: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture by remember {
        mutableStateOf(ProcessCameraProvider.getInstance(context))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    AndroidView(modifier = modifier, factory = {
        val previewView = PreviewView(it).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
        val executor = Executors.newSingleThreadExecutor()
        cameraProviderFuture.addListener({
            bindCameraUseCases(
                lifecycleOwner = lifecycleOwner,
                cameraProviderFuture = cameraProviderFuture,
                executor = executor,
                previewView = previewView,
                onImageAnalyzed = onImageAnalyzed
            )
        }, ContextCompat.getMainExecutor(context))

        previewView
    })
}

fun bindCameraUseCases(
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    executor: ExecutorService,
    previewView: PreviewView,
    onImageAnalyzed: (ImageProxy) -> Unit,
) {
    val preview: Preview = Preview.Builder().setTargetAspectRatio(RATIO_4_3).build()

    preview.surfaceProvider = previewView.surfaceProvider

    val cameraSelector: CameraSelector =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    val imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(RATIO_4_3)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
    imageAnalysis.setAnalyzer(executor, onImageAnalyzed)

    val cameraProvider = cameraProviderFuture.get()
    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis, preview)
}

@Composable
private fun AnalysisOverlay(uiState: UiState) {
    if (uiState.binaryMask == null) {
        return
    }
    val maskOverlay = replaceColor(uiState.binaryMask, Color.Black, Color.Transparent)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawImage(
            maskOverlay.scale(size.width.toInt(), size.height.toInt()).asImageBitmap(),
            colorFilter = ColorFilter.tint(Color(0x8000FF00), BlendMode.SrcIn)
        )
        if (uiState.documentQuad != null) {
            val scaledQuad = uiState.documentQuad.scaledTo(
                fromWidth = uiState.binaryMask.width,
                fromHeight = uiState.binaryMask.height,
                toWidth = size.width.toInt(),
                toHeight = size.height.toInt()
            )
            scaledQuad.edges().forEach {
                drawLine(Color.Green, it.from.toOffset(), it.to.toOffset(), 5.0f)
            }
        }
    }
}

fun replaceColor(bitmap: Bitmap, toReplace: Color, replacement: Color): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height)

    val target = toReplace.toArgb()
    val newColor = replacement.toArgb()

    for (i in pixels.indices) {
        if (pixels[i] == target) {
            pixels[i] = newColor
        }
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

fun Point.toOffset() = Offset(x.toFloat(), y.toFloat())
