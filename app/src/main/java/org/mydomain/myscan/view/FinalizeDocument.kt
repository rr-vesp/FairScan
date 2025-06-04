package org.mydomain.myscan.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.mydomain.myscan.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeDocumentScreen(
    viewModel: MainViewModel = viewModel(),
    onBackPressed: () -> Unit,
    onSavePressed: () -> Unit
) {
    val pageIds by viewModel.pageIds.collectAsStateWithLifecycle()
    Scaffold (
        topBar = {
            TopAppBar(
                title = { Text("Finalize document") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = onSavePressed) {
                        Text(text = "Save PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            Text(
                "Pages",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow (
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pageIds.forEachIndexed { index, id ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = viewModel.getBitmap(id).asImageBitmap(),
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text("Page ${index + 1}")
                    }
                }
            }
        }
    }
}
