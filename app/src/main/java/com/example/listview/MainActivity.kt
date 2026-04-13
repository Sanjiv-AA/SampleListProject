package com.example.listview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.listview.ui.theme.ListViewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListViewTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Show the list screen (ViewModel-backed)
                    ListScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ListViewTheme {
        ListScreen()
    }
}