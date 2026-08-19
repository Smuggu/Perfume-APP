package com.scentvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.scentvault.app.ui.ScentVaultNavHost
import com.scentvault.app.ui.theme.ScentVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScentVaultRoot()
        }
    }
}

@Composable
private fun ScentVaultRoot() {
    ScentVaultTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScentVaultNavHost()
        }
    }
}
