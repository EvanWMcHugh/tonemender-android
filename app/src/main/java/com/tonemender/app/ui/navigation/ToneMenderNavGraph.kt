package com.tonemender.app.ui.navigation

import androidx.compose.runtime.*
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

        // Auth
        composable(AppScreen.SignIn.route) {
            SignInScreen(
                onGoToSignUp = {
                    navController.navigate(AppScreen.SignUp.route)
                },
                onGoToForgotPassword = { email ->
                    navController.navigate(
                        AppScreen.ForgotPassword.createRoute(email)
                    )
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
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            ForgotPasswordScreen(
                prefilledEmail = email,
                onBack = { navController.popBackStack() }
            )
        }

        // Main App
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
                    sessionViewModel.signOut()
                },
                sessionViewModel = sessionViewModel
            )
        }

        // Account Actions
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

        // Billing
        composable(AppScreen.Upgrade.route) {
            UpgradeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Prevent duplicate destinations in back stack
 */
private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}