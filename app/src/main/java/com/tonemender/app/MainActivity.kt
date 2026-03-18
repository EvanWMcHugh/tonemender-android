package com.tonemender.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.tonemender.app.ui.common.UiMessageManager
import com.tonemender.app.ui.navigation.AppScreen
import com.tonemender.app.ui.navigation.ToneMenderNavGraph
import com.tonemender.app.ui.session.SessionViewModel
import com.tonemender.app.ui.theme.ToneMenderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToneMenderTheme {
                val sessionViewModel: SessionViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }

                val isReady by sessionViewModel.isReady.collectAsState()
                val isSignedIn by sessionViewModel.isSignedIn.collectAsState()

                LaunchedEffect(Unit) {
                    UiMessageManager.messages.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        }
                    ) { innerPadding ->
                        if (!isReady) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading ToneMender…",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            key(isSignedIn) {
                                val navController = rememberNavController()

                                ToneMenderNavGraph(
                                    navController = navController,
                                    sessionViewModel = sessionViewModel,
                                    startDestination = if (isSignedIn) {
                                        AppScreen.Rewrite.route
                                    } else {
                                        AppScreen.SignIn.route
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}