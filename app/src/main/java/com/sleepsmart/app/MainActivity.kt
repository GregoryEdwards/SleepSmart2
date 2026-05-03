package com.sleepsmart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sleepsmart.app.ui.nav.AppNav
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.SleepSmartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Navy950.value.toInt()),
            navigationBarStyle = SystemBarStyle.dark(Navy950.value.toInt())
        )
        super.onCreate(savedInstanceState)
        setContent {
            SleepSmartTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Navy950)) {
                    AppNav()
                }
            }
        }
    }
}
