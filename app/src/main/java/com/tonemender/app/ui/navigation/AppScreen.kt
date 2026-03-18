package com.tonemender.app.ui.navigation

import android.net.Uri

sealed class AppScreen(val route: String) {
    data object Rewrite : AppScreen("rewrite")
    data object Drafts : AppScreen("drafts")
    data object Account : AppScreen("account")
    data object Upgrade : AppScreen("upgrade")
    data object SignIn : AppScreen("sign_in")
    data object SignUp : AppScreen("sign_up")

    data object ForgotPassword : AppScreen("forgot_password?email={email}") {
        fun createRoute(email: String): String {
            return "forgot_password?email=${Uri.encode(email)}"
        }
    }

    data object ChangeEmail : AppScreen("change_email")
    data object DeleteAccount : AppScreen("delete_account")
}