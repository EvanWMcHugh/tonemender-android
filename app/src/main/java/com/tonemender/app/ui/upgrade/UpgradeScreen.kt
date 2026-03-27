package com.tonemender.app.ui.upgrade

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    viewModel: UpgradeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UpgradeHeader(
            title = uiState.title,
            description = uiState.description
        )

        PlanSelectionCard(
            uiState = uiState,
            onSelectPlan = viewModel::selectPlan
        )

        UpgradeFeaturesCard()

        uiState.errorMessage?.let {
            ErrorText(it)
        }

        PurchaseButton(
            uiState = uiState,
            activity = activity,
            onStartPurchase = { safeActivity ->
                viewModel.startPurchase(safeActivity)
            }
        )

        OutlinedButton(
            onClick = viewModel::refreshPlans,
            enabled = !uiState.isLoading && !uiState.isPurchasing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Plans")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun UpgradeHeader(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PlanSelectionCard(
    uiState: UpgradeUiState,
    onSelectPlan: (UpgradePlan) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiState.productName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Choose a plan",
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.isLoading && !uiState.hasPlans) {
                CircularProgressIndicator()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.monthlyPriceLabel.isNotBlank()) {
                        FilterChip(
                            selected = uiState.selectedPlan == UpgradePlan.MONTHLY,
                            onClick = { onSelectPlan(UpgradePlan.MONTHLY) },
                            label = { Text("Monthly • ${uiState.monthlyPriceLabel}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.yearlyPriceLabel.isNotBlank()) {
                        FilterChip(
                            selected = uiState.selectedPlan == UpgradePlan.YEARLY,
                            onClick = { onSelectPlan(UpgradePlan.YEARLY) },
                            label = { Text("Yearly • ${uiState.yearlyPriceLabel}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeFeaturesCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "What you unlock",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "• Unlimited rewrites",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Tone selection",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Recipient targeting",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Premium rewrite experience",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PurchaseButton(
    uiState: UpgradeUiState,
    activity: Activity?,
    onStartPurchase: (Activity) -> Unit
) {
    val enabled = activity != null &&
            uiState.hasPlans &&
            !uiState.isLoading &&
            !uiState.isPurchasing

    Button(
        onClick = {
            activity?.let(onStartPurchase)
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (uiState.isPurchasing) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text(buildPurchaseButtonLabel(uiState))
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error
    )
}

private fun buildPurchaseButtonLabel(uiState: UpgradeUiState): String {
    val selectedPrice = when (uiState.selectedPlan) {
        UpgradePlan.MONTHLY -> uiState.monthlyPriceLabel
        UpgradePlan.YEARLY -> uiState.yearlyPriceLabel
    }

    return if (selectedPrice.isNotBlank()) {
        "Continue with $selectedPrice"
    } else {
        "Continue"
    }
}