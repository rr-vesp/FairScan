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

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mydomain.myscan.GeneratedPdf
import org.mydomain.myscan.PdfGenerationActions
import org.mydomain.myscan.ui.theme.MyScanTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfGenerationDialogWrapper(
    onDismiss: () -> Unit,
    pdfActions: PdfGenerationActions,
) {
    var filename by remember { mutableStateOf(defaultFilename()) }
    var isGenerating by remember { mutableStateOf(true) }
    var pdf by remember { mutableStateOf<GeneratedPdf?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        job = coroutineScope.launch {
            try {
                val result = pdfActions.generatePdf()
                pdf = result
                isGenerating = false
            } catch (_: CancellationException) {
                // Cancelled
            } catch (_: Exception) {
                // Error
                isGenerating = false
            }
        }
    }

    PdfGenerationDialog(
        filename = filename,
        onFilenameChange = { filename = it },
        isGenerating = isGenerating,
        pdf = pdf,
        onDismiss = {
            job?.cancel()
            onDismiss()
        },
        onShare = { pdf?.uri?.let(pdfActions.onShare) },
        onSave = { pdf?.uri?.let(pdfActions.onSave) },
        onOpen = { pdf?.uri?.let(pdfActions.onOpen) }
    )
}

@Composable
fun PdfGenerationDialog(
    filename: String,
    onFilenameChange: (String) -> Unit,
    isGenerating: Boolean,
    pdf: GeneratedPdf?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = onFilenameChange,
                    label = { Text("Filename") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating PDF…")
                    }
                } else if (pdf != null) {
                    val context = LocalContext.current
                    Text("${pdf.pageCount} pages – ${formatFileSize(pdf.sizeInBytes, context)}")
                }
            }
        },
        // TODO 4 buttons (counting dismissButton): that's too many
        confirmButton = {
            if (!isGenerating && pdf != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onShare) { Text("Share") }
                    TextButton(onClick = onSave) { Text("Save") }
                    TextButton(onClick = onOpen) { Text("Open") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

fun defaultFilename(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "scan_$timestamp.pdf"
}

fun formatFileSize(sizeInBytes: Long?, context: Context): String {
    return if (sizeInBytes == null) "Unknown size"
    else Formatter.formatShortFileSize(context, sizeInBytes)
}

@Preview
@Composable
fun PreviewPdfGenerationDialogDuringGeneration() {
    MyScanTheme {
        PdfGenerationDialog(
            filename = "scan_20250702_174042.pdf",
            isGenerating = true,
            pdf = null,
            onFilenameChange = {},
            onDismiss = {},
            onShare = {},
            onSave = {},
            onOpen = {}
        )
    }
}

@Preview
@Composable
fun PreviewPdfGenerationDialogAfterGeneration() {
    MyScanTheme {
        PdfGenerationDialog(
            filename = "scan_20250702_174042.pdf",
            isGenerating = false,
            pdf = GeneratedPdf("file://fake.pdf".toUri(), 42897L, 3),
            onFilenameChange = {},
            onDismiss = {},
            onShare = {},
            onSave = {},
            onOpen = {}
        )
    }
}
