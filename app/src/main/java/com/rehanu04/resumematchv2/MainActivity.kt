package com.rehanu04.resumematchv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rehanu04.resumematchv2.nav.AppNav
import com.rehanu04.resumematchv2.ui.theme.ResumeMatchV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(true) }

            // ✅ Your backend (Render) API base URL:
            val apiBaseUrl = "https://resumematch-ai-backend.onrender.com/"

            // ✅ Put YOUR Railway X-App-Key here (same one that works in PowerShell):
            val apiAppKey = ""

            ResumeMatchV2Theme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        darkMode = darkMode,
                        onToggleDark = { darkMode = it },
                        apiBaseUrl = apiBaseUrl,
                        apiAppKey = apiAppKey
                    )
                }
            }
        }
    }
}
