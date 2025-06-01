package org.mydomain.myscan

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mydomain.myscan.ui.theme.MyScanTheme
import org.mydomain.myscan.view.CameraScreen
import org.mydomain.myscan.view.PagePreviewScreen
import org.opencv.android.OpenCVLoader
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOpenCV()
        val viewModel: MainViewModel by viewModels { MainViewModel.getFactory(this) }
        enableEdgeToEdge()
        setContent {
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val cameraScreenState by viewModel.cameraScreenState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            MyScanTheme {
                Scaffold { innerPadding ->
                    Column (modifier = Modifier.padding(innerPadding)) {
                        when (val screen = currentScreen) {
                            is Screen.Camera -> {
                                CameraScreen(viewModel, cameraScreenState,
                                    onImageAnalyzed = { image -> viewModel.segment(image) } )
                            }
                            is Screen.PagePreview -> {
                                PagePreviewScreen (
                                    image = screen.image,
                                    isProcessing = screen.isProcessing,
                                    onBackPressed = { viewModel.navigateTo(Screen.Camera) },
                                    onSavePressed = createPdfAndShare(context)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createPdfAndShare(context: Context): (Bitmap) -> Unit = { bitmap ->
        val outputDir = File(cacheDir, "pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, "scan_${System.currentTimeMillis()}.pdf")
        val success = createPdfFromBitmaps(listOf(bitmap), outputFile)
        if (success) {
            val uri = FileProvider.getUriForFile(
                context,
                "${applicationContext.packageName}.fileprovider",
                outputFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } else {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initOpenCV() {
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.d("OpenCV", "Initialization successful")
        }
    }
}
