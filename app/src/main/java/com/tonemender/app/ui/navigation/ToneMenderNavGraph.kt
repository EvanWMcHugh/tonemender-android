package com.tonemender.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tonemender.app.data.local.drafts.Draft
import com.tonemender.app.ui.account.AccountScreen
import com.tonemender.app.ui.account.ChangeEmailScreen
import com.tonemender.app.ui.account.DeleteAccountScreen
import com.tonemender.app.ui.auth.ForgotPasswordScreen
import com.tonemender.app.ui.auth.SignInScreen
import com.tonemender.app.ui.auth.SignUpScreen
import com.tonemender.app.ui.drafts.DraftsScreen
import com.tonemender.app.ui.rewrite.RewriteScreen
import com.tonemender.app.ui.session.SessionViewModel
import com.tonemender.app.ui.upgrade.UpgradeScreen

@Composable
fun ToneMenderNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = AppScreen.SignIn.route
) {
    var pendingDraft by remember { mutableStateOf<Draft?>(null) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppScreen.SignIn.route) {
            SignInScreen(
                onGoToSignUp = { navController.navigate(AppScreen.SignUp.route) },
                onGoToForgotPassword = { email ->
                    navController.navigate(AppScreen.ForgotPassword.createRoute(email))
                },
                sessionViewModel = sessionViewModel
            )
        }

        composable(AppScreen.SignUp.route) {
            SignUpScreen(
                onGoToSignIn = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.ForgotPassword.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            )
        ) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppScreen.Rewrite.route) {
            RewriteScreen(
                onGoToDrafts = {
                    navController.navigateSingleTopTo(AppScreen.Drafts.route)
                },
                onGoToAccount = {
                    navController.navigateSingleTopTo(AppScreen.Account.route)
                },
                onGoToUpgrade = {
                    navController.navigateSingleTopTo(AppScreen.Upgrade.route)
                },
                initialDraft = pendingDraft,
                onDraftConsumed = {
                    pendingDraft = null
                }
            )
        }

        composable(AppScreen.Drafts.route) {
            DraftsScreen(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draft ->
                    pendingDraft = draft
                    navController.navigate(AppScreen.Rewrite.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppScreen.Account.route) {
            AccountScreen(
                onBack = { navController.popBackStack() },
                onGoToUpgrade = {
                    navController.navigateSingleTopTo(AppScreen.Upgrade.route)
                },
                onGoToChangeEmail = {
                    navController.navigate(AppScreen.ChangeEmail.route)
                },
                onGoToDeleteAccount = {
                    navController.navigate(AppScreen.DeleteAccount.route)
                },
                onSignOut = {
                    navController.navigate(AppScreen.SignIn.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                sessionViewModel = sessionViewModel
            )
        }

        composable(AppScreen.ChangeEmail.route) {
            ChangeEmailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppScreen.DeleteAccount.route) {
            DeleteAccountScreen(
                onBack = { navController.popBackStack() },
                onDeleted = {
                    sessionViewModel.signOut()
                }
            )
        }

        composable(AppScreen.Upgrade.route) {
            UpgradeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}