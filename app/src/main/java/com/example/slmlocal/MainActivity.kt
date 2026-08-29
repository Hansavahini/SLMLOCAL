package com.example.slmlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.slmlocal.ui.ChatScreen
import com.example.slmlocal.ui.theme.SLMLOCALTheme

import androidx.activity.viewModels
import com.example.slmlocal.ui.ChatViewModel
import com.example.slmlocal.ui.IngestionViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val ingestionViewModel: IngestionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SLMLOCALTheme {
                ChatScreen(chatViewModel, ingestionViewModel)
            }
        }
    }
}