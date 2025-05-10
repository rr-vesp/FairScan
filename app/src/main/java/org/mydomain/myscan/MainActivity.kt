package org.mydomain.myscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.mydomain.myscan.ui.theme.MyScanTheme
import org.mydomain.myscan.view.CameraScreen
import java.util.Date

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MyScan"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyScanTheme {
                Scaffold(/*modifier = Modifier.fillMaxSize()*/) { innerPadding ->
                    Column {
                        Greeting(modifier = Modifier.padding(innerPadding))
                        Box(/*modifier = Modifier.width(300.dp)*/) {
                            CameraScreen(onImageAnalyzed = { image ->
                                Log.d(TAG, Date().toString())
                                image.close()
                            } )
                        }
                    }
                }
            }
        }
    }
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
fun GreetingPreview() {
    MyScanTheme {
        Greeting()
    }
}