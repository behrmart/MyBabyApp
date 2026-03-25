package com.example.mybabyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mybabyapp.ui.navigation.VideoLockerApp
import com.example.mybabyapp.ui.theme.MyBabyAppTheme
import com.example.mybabyapp.util.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = AppContainer.from(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyBabyAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoLockerApp(
                        sessionStore = appContainer.sessionStore,
                        adminRepository = appContainer.adminRepository,
                        authRepository = appContainer.authRepository,
                        photosRepository = appContainer.photosRepository,
                        serverMediaRepository = appContainer.serverMediaRepository,
                        videosRepository = appContainer.videosRepository,
                        okHttpClient = appContainer.okHttpClient
                    )
                }
            }
        }
    }
}
