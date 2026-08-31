package com.example.gemma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gemma.ui.ChatScreen
import com.example.gemma.ui.theme.gemmaTheme

import androidx.activity.viewModels
import com.example.gemma.ui.ChatViewModel
import com.example.gemma.ui.IngestionViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val ingestionViewModel: IngestionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            gemmaTheme {
                ChatScreen(chatViewModel, ingestionViewModel)
            }
        }
    }
}