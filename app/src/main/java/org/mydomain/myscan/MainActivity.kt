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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            // TODO or collectAsStateWithLifecycle()?
            val currentScreen by viewModel.currentScreen.collectAsState()
            // TODO should uiState own currentScreen?
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            MyScanTheme {
                Scaffold { innerPadding ->
                    Column {
                        Greeting(modifier = Modifier.padding(innerPadding))
                        MyMessageBox(uiState.detectionMessage, uiState.inferenceTime)
                        when (val screen = currentScreen) {
                            is Screen.Camera -> {
                                CameraScreen(viewModel, uiState,
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

@Composable
fun MyMessageBox(msg: String?, inferenceTime: Long) {
    Text(
        text = (msg ?: "") + " / inferred in " + inferenceTime + "ms",
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Gray)
            .fillMaxWidth(),
        color = Color.Black,
    )
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Text(
        text = "Scan your document",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MyMessageBoxPreview() {
    MyScanTheme {
        MyMessageBox("Found 2 objects!", 42)
    }
}