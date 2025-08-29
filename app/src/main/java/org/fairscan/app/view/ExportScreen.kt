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
package org.fairscan.app.view

import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.fairscan.app.GeneratedPdf
import org.fairscan.app.Navigation
import org.fairscan.app.PdfGenerationActions
import org.fairscan.app.R
import org.fairscan.app.ui.PdfGenerationUiState
import org.fairscan.app.ui.theme.MyScanTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreenWrapper(
    navigation: Navigation,
    pdfActions: PdfGenerationActions,
    onCloseScan: () -> Unit,
) {
    BackHandler { navigation.back() }

    val showConfirmationDialog = rememberSaveable { mutableStateOf(false) }
    val filename = remember { mutableStateOf(defaultFilename()) }
    val uiState by pdfActions.uiStateFlow.collectAsState()
    LaunchedEffect(Unit) {
        pdfActions.setFilename(filename.value)
        pdfActions.startGeneration()
    }

    val onFilenameChange = { newName:String ->
        filename.value = newName
        pdfActions.setFilename(newName)
    }
    val ensureCorrectFileName = {
        val value = filename.value.trim().ifEmpty { defaultFilename() }
        if (value != filename.value) {
            onFilenameChange(value)
        }
    }

    ExportScreen(
        filename = filename,
        onFilenameChange = onFilenameChange,
        uiState = uiState,
        navigation = navigation,
        onShare = {
            ensureCorrectFileName()
            pdfActions.sharePdf()
        },
        onSave = {
            ensureCorrectFileName()
            pdfActions.savePdf()
        },
        onOpen = { pdfActions.openPdf() },
        onCloseScan = {
            if (uiState.hasSavedOrSharedPdf)
                onCloseScan()
            else
                showConfirmationDialog.value = true
        },
    )

    if (showConfirmationDialog.value) {
        NewDocumentDialog(onCloseScan, showConfirmationDialog, stringResource(R.string.end_scan))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    filename: MutableState<String>,
    onFilenameChange: (String) -> Unit,
    uiState: PdfGenerationUiState,
    navigation: Navigation,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onOpen: () -> Unit,
    onCloseScan: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_pdf)) },
                navigationIcon = { BackButton(navigation.back) },
                actions = {
                    AboutScreenNavButton(onClick = navigation.toAboutScreen)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val focusRequester = remember { FocusRequester() }
            OutlinedTextField(
                value = filename.value,
                onValueChange = onFilenameChange,
                label = { Text(stringResource(R.string.filename)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                trailingIcon = {
                    if (filename.value.isNotEmpty()) {
                        IconButton(onClick = {
                            filename.value = ""
                            focusRequester.requestFocus()
                        }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.clear_text))
                        }
                    }
                },
            )

            val pdf = uiState.generatedPdf

            // PDF infos
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                if (uiState.isGenerating) {
                    Text(stringResource(R.string.creating_pdf), fontStyle = FontStyle.Italic)
                } else if (pdf != null) {
                    val context = LocalContext.current
                    val formattedFileSize = formatFileSize(pdf.sizeInBytes, context)
                    Text(text = pageCountText(pdf.pageCount))
                    Text(
                        text = stringResource(R.string.file_size, formattedFileSize),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (uiState.saveDirectoryName != null) {
                SavePdfBar(onOpen, uiState.saveDirectoryName)
            }
            if (uiState.errorMessage != null) {
                ErrorBar(uiState.errorMessage)
            }

            Spacer(Modifier.weight(1f)) // push buttons down

            // Export actions
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActions(pdf, onShare, onSave)

                OutlinedButton(
                    onClick = onCloseScan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.end_scan))
                }
            }
        }
    }
}

@Composable
private fun MainActions(
    pdf: GeneratedPdf?,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MainActionButton(
            onClick = onShare,
            enabled = pdf != null,
            icon = Icons.Default.Share,
            iconDescription = stringResource(R.string.share),
            text = stringResource(R.string.share),
            modifier = Modifier.weight(1f)
        )
        MainActionButton(
            onClick = onSave,
            enabled = pdf != null,
            icon = Icons.Default.Download,
            iconDescription = stringResource(R.string.save),
            text = stringResource(R.string.save),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SavePdfBar(onOpen: () -> Unit, saveDirectoryName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.pdf_saved_to, saveDirectoryName),
            style = MaterialTheme.typography.bodyMedium
        )
        MainActionButton(
            onClick = onOpen,
            text = stringResource(R.string.open),
            icon = Icons.AutoMirrored.Filled.OpenInNew,
        )
    }
}

@Composable
private fun ErrorBar(errorMessage: String) {
    Text(
        text = stringResource(R.string.error, errorMessage),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    )
}

fun defaultFilename(): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault()).format(Date())
    return "Scan $timestamp"
}

fun formatFileSize(sizeInBytes: Long?, context: Context): String {
    return if (sizeInBytes == null) context.getString(R.string.unknown_size)
    else Formatter.formatShortFileSize(context, sizeInBytes)
}

@Preview
@Composable
fun PreviewExportScreenDuringGeneration() {
    ExportPreviewToCustomize(
        uiState = PdfGenerationUiState(isGenerating = true)
    )
}

@Preview
@Composable
fun PreviewExportScreenAfterSave() {
    val file = File("fake.pdf")
    ExportPreviewToCustomize(
        uiState = PdfGenerationUiState(
            generatedPdf = GeneratedPdf(file, 442897L, 3),
            savedFileUri = file.toUri(),
            saveDirectoryName = "Downloads",
        ),
    )
}

@Preview
@Composable
fun ExportScreenPreviewWithError() {
    ExportPreviewToCustomize(
        PdfGenerationUiState(errorMessage = "PDF generation failed")
    )
}

@Composable
fun ExportPreviewToCustomize(uiState: PdfGenerationUiState) {
    MyScanTheme {
        ExportScreen(
            filename = remember { mutableStateOf("Scan 2025-07-02 17.40.42") },
            onFilenameChange = {_->},
            navigation = dummyNavigation(),
            uiState = uiState,
            onShare = {},
            onSave = {},
            onOpen = {},
            onCloseScan = {},
        )
    }
}

