package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.configureAppCheck
import com.example.ui.navigation.ParijotoPhase2NavGraph
import com.example.ui.theme.ParijotoLearningTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check before ViewModels / Firestore / AI Logic calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            ParijotoLearningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParijotoPhase2NavGraph()
                }
            }
        }
    }
}
