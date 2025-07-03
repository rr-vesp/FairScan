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
package org.mydomain.myscan

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mydomain.myscan.ui.theme.MyScanTheme
import org.mydomain.myscan.view.CameraScreen
import org.mydomain.myscan.view.DocumentScreen
import org.opencv.android.OpenCVLoader
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLibraries()
        val viewModel: MainViewModel by viewModels { MainViewModel.getFactory(this) }
        enableEdgeToEdge()
        setContent {
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val liveAnalysisState by viewModel.liveAnalysisState.collectAsStateWithLifecycle()
            val pageIds by viewModel.pageIds.collectAsStateWithLifecycle()
            MyScanTheme {
                when (val screen = currentScreen) {
                    is Screen.Camera -> {
                        CameraScreen(
                            viewModel,
                            liveAnalysisState,
                            onImageAnalyzed = { image -> viewModel.liveAnalysis(image) },
                            onFinalizePressed = { viewModel.navigateTo(Screen.FinalizeDocument()) },
                        )
                    }
                    is Screen.FinalizeDocument -> {
                        DocumentScreen (
                            pageIds,
                            initialPage = screen.initialPage,
                            imageLoader = { id -> viewModel.getBitmap(id) },
                            toCameraScreen = { viewModel.navigateTo(Screen.Camera) },
                            // TODO Save and share files with the filename chosen by the user
                            pdfActions = PdfGenerationActions(
                                generatePdf = viewModel::generatePdf,
                                onShare = { uri -> sharePdf(uri) },
                                onSave = { uri -> savePdf(uri) },
                                onOpen = { uri -> savePdf(uri) /* TODO Open */}
                            ),
                            onStartNew = {
                                viewModel.startNewDocument()
                                viewModel.navigateTo(Screen.Camera) },
                            onDeleteImage =  { id -> viewModel.deletePage(id) }
                        )
                    }
                }
            }
        }
    }

    private fun sharePdf(fileUri: Uri) {
        val fileUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            fileUri.toFile()
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    }

    private fun savePdf(fileUri: Uri) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val generatedFile = fileUri.toFile()
        val targetFile = File(downloadsDir, generatedFile.name)
        generatedFile.copyTo(targetFile)
        MediaScannerConnection.scanFile(
            this, arrayOf(targetFile.absolutePath), arrayOf("application/pdf"), null
        )
        Toast.makeText(this, "Saved PDF in Downloads", Toast.LENGTH_SHORT).show()
    }

    private fun initLibraries() {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.d("OpenCV", "Initialization successful")
        }
    }
}
