// /app/src/main/java/com/daytradchat/papa/MainActivity.kt
// ver 1.00-02
package com.daytradchat.papa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.daytradchat.papa.ui.DisplayApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DisplayApp(viewModel = viewModel)
        }
    }
}
