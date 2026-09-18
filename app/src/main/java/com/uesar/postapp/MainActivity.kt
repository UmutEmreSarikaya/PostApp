package com.uesar.postapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.uesar.postapp.post.presentation.list.PostListScreen
import com.uesar.postapp.ui.theme.PostAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PostAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PostListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}