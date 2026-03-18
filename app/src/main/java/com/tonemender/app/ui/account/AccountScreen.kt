package com.tonemender.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonemender.app.ui.session.SessionViewModel

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onGoToUpgrade: () -> Unit,
    onGoToChangeEmail: () -> Unit,
    onGoToDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    sessionViewModel: SessionViewModel,
    accountViewModel: AccountViewModel = viewModel()
) {
    val uiState by accountViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        accountViewModel.loadAccount()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accountViewModel.loadAccount()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "View your plan and usage",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.loading) {
            CircularProgressIndicator()
        } else {
            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.email ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Plan",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = if (uiState.isPro) {
                            uiState.planType ?: "Pro"
                        } else {
                            "Free"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Usage",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Rewrites today: ${uiState.rewritesToday}")
                    Text("Total rewrites: ${uiState.totalRewrites}")
                }
            }

            OutlinedButton(
                onClick = onGoToChangeEmail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Email")
            }

            OutlinedButton(
                onClick = onGoToDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Account")
            }

            if (!uiState.isPro) {
                Button(
                    onClick = onGoToUpgrade,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade to Pro")
                }
            }

            OutlinedButton(
                onClick = {
                    sessionViewModel.signOut()
                    onSignOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}