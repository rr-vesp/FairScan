package org.mydomain.myscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mydomain.myscan.ui.theme.MyScanTheme
import org.mydomain.myscan.view.CameraScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels { MainViewModel.getFactory(this) }
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyScanTheme {
                Scaffold { innerPadding ->
                    Column {
                        Greeting(modifier = Modifier.padding(innerPadding))
                        MyMessageBox(uiState.detectionMessage, uiState.inferenceTime)
                        Box {
                            CameraScreen(uiState, onImageAnalyzed = { image -> viewModel.segment(image) } )
                        }
                    }
                }
            }
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