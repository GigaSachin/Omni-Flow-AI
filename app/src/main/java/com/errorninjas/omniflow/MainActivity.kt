package com.errorninjas.omniflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.errorninjas.omniflow.ui.OmniFlowScreen
import com.errorninjas.omniflow.ui.theme.OmniFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniFlowTheme {
                OmniFlowScreen()
            }
        }
    }
}
