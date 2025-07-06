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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.mydomain.myscan.PdfGenerationActions
import org.mydomain.myscan.ui.PdfGenerationUiState
import org.mydomain.myscan.ui.theme.MyScanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    pageIds: List<String>,
    initialPage: Int,
    imageLoader: (String) -> Bitmap?,
    toCameraScreen: () -> Unit,
    pdfActions: PdfGenerationActions,
    onStartNew: () -> Unit,
    onDeleteImage: (String) -> Unit,
) {
    // TODO Check how often images are loaded
    val showNewDocDialog = rememberSaveable { mutableStateOf(false) }
    val showPdfDialog = rememberSaveable { mutableStateOf(false) }
    val currentPageIndex = rememberSaveable { mutableIntStateOf(initialPage) }
    if (currentPageIndex.intValue >= pageIds.size) {
        currentPageIndex.intValue = pageIds.size - 1
    }
    if (currentPageIndex.intValue < 0) {
        toCameraScreen()
        return
    }
    Scaffold (
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = { Text("Document") },
                navigationIcon = {
                    IconButton(onClick = toCameraScreen) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                PageList(pageIds, imageLoader, currentPageIndex, toCameraScreen)
                BottomBar(showPdfDialog, showNewDocDialog)
            }
        }
    ) { padding ->
        DocumentPreview(pageIds, imageLoader, currentPageIndex, onDeleteImage, padding)
        if (showNewDocDialog.value) {
            NewDocumentDialog(onConfirm = onStartNew, showNewDocDialog)
        }
        if (showPdfDialog.value) {
            PdfGenerationBottomSheetWrapper(
                onDismiss = { showPdfDialog.value = false },
                pdfActions = pdfActions,
            )
        }
    }
}

@Composable
private fun DocumentPreview(
    pageIds: List<String>,
    imageLoader: (String) -> Bitmap?,
    currentPageIndex: MutableIntState,
    onDeleteImage: (String) -> Unit,
    padding: PaddingValues,
) {
    val imageId = pageIds[currentPageIndex.intValue]
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(padding)
    ) {
        Box (
            modifier = Modifier.fillMaxSize()
        ) {
            val bitmap = imageLoader(imageId)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                val zoomState = rememberZoomState(
                    contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                )

                LaunchedEffect(imageId) {
                    zoomState.reset()
                }
                Box(modifier = Modifier.fillMaxSize(0.92f).align(Alignment.Center)) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .zoomable(zoomState)
                    )
                }
            }
            MyIconButton(
                Icons.Outlined.Delete,
                contentDescription = "Delete page",
                onClick = { onDeleteImage(imageId) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            Text("${currentPageIndex.intValue + 1} / ${pageIds.size}",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(all = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PageList(
    pageIds: List<String>,
    imageLoader: (String) -> Bitmap?,
    currentPageIndex: MutableState<Int>,
    toCameraScreen: () -> Unit
) {
    Box {
        CommonPageList(
            pageIds,
            imageLoader,
            onPageClick = { index -> currentPageIndex.value = index },
            currentPageIndex = currentPageIndex.value,
        )
        MyIconButton(
            icon = Icons.Default.Add,
            onClick = toCameraScreen,
            contentDescription = "Add page",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun BottomBar(
    showPdfDialog: MutableState<Boolean>,
    showNewDocDialog: MutableState<Boolean>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Button(onClick = { showPdfDialog.value = true }) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "Generate PDF")
            Spacer(Modifier.width(8.dp))
            Text("Generate PDF")
        }
        Spacer(modifier = Modifier.width(8.dp))
        MyIconButton(
            icon = Icons.Default.RestartAlt,
            contentDescription = "Restart",
            onClick = { showNewDocDialog.value = true },
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun NewDocumentDialog(onConfirm: () -> Unit, showDialog: MutableState<Boolean>) {
    AlertDialog(
        title = { Text("New document") },
        text = { Text("The current document will be lost if you haven't saved it. Do you want to continue?") },
        confirmButton = {
            TextButton (onClick = {
                showDialog.value = false
                onConfirm()
            }) {
                Text("Yes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog.value = false }) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        },
        onDismissRequest = { showDialog.value = false },
    )
}

@Composable
fun MyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton (
        onClick = onClick,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
@Preview
fun DocumentScreenPreview() {
    val context = LocalContext.current
    MyScanTheme {
        DocumentScreen(
            pageIds = listOf(1, 2, 2, 2).map { "gallica.bnf.fr-bpt6k5530456s-$it.jpg" },
            initialPage = 1,
            imageLoader = { id ->
                context.assets.open(id).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            },
            toCameraScreen = {},
            pdfActions = PdfGenerationActions(
                {}, {}, {},
                MutableStateFlow(PdfGenerationUiState()),
                {}, {}, {}),
            onStartNew = {},
            onDeleteImage = { _ -> {} }
        )
    }
}
