//app/src/main/java/com/daytradchat/papa/MainActivity.kt
//ver 1.10-11
package com.daytradchat.papa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daytradchat.papa.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            MainScreen(vm)
        }
    }
}
