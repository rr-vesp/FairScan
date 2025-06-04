package org.mydomain.myscan

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
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
import org.mydomain.myscan.view.FinalizeDocumentScreen
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOpenCV()
        val viewModel: MainViewModel by viewModels { MainViewModel.getFactory(this) }
        enableEdgeToEdge()
        setContent {
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val liveAnalysisState by viewModel.liveAnalysisState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            MyScanTheme {
                Scaffold { innerPadding ->
                    Column (modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            is Screen.Camera -> {
                                CameraScreen(viewModel, liveAnalysisState,
                                    onImageAnalyzed = { image -> viewModel.segment(image) },
                                    onFinalizePressed = { viewModel.navigateTo(Screen.FinalizeDocument) }
                                )
                            }
                            is Screen.FinalizeDocument -> {
                                FinalizeDocumentScreen (
                                    viewModel,
                                    onBackPressed = { viewModel.navigateTo(Screen.Camera) },
                                    onSavePressed = savePdf(viewModel, context),
                                    // TODO "on share"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /*
    private fun createPdfAndShare(context: Context): (Bitmap) -> Unit = { bitmap ->
        val outputDir = File(cacheDir, "pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, "scan_${System.currentTimeMillis()}.pdf")
        val document = createPdfFromBitmaps(listOf(bitmap))
        var success = true
        try {
            FileOutputStream(outputFile).use { outputStream ->
                document.writeTo(outputStream)
            }
        } catch (_: IOException) {
            Toast.makeText(context, "Failed to share PDF", Toast.LENGTH_SHORT).show()
            success = false
        } finally {
            document.close()
        }
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
        }
    }
     */

    private fun savePdf(
        viewModel: MainViewModel,
        context: Context
    ): () -> Unit = {
        val document = viewModel.createPdf()
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, "scan_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf("application/pdf"), null
            )

            Toast.makeText(context, "Saved PDF in Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MyScan", "Failed to save PDF", e)
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
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
