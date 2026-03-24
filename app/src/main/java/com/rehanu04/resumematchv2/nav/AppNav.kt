package com.rehanu04.resumematchv2.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.ui.ProfileSetupScreen
import com.rehanu04.resumematchv2.ui.AiAssistantScreen // ✅ Imported the new screen

@Composable
fun AppNav(
    darkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    apiBaseUrl: String,
    apiAppKey: String
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val userProfileStore = remember { UserProfileStore(context) }

    NavHost(navController = nav, startDestination = Routes.ANALYZE) {
        composable(Routes.ANALYZE) {
            com.rehanu04.resumematchv2.ui.AnalyzeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onGoCreate = { nav.navigate(Routes.CREATE) },
                onGoProfile = { nav.navigate(Routes.PROFILE) },
                apiBaseUrl = apiBaseUrl,
                apiAppKey = apiAppKey,
                userProfileStore = userProfileStore
            )
        }
        composable(Routes.CREATE) {
            com.rehanu04.resumematchv2.ui.CreateResumeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { nav.popBackStack() },
                onGoAiAssistant = { nav.navigate(Routes.AI_ASSISTANT) },
                onGoProfile = { nav.navigate(Routes.PROFILE) }, // ✅ Now it knows how to open the profile!
                apiBaseUrl = apiBaseUrl,
                apiAppKey = apiAppKey,
                userProfileStore = userProfileStore
            )
        }
        composable(Routes.PROFILE) {
            ProfileSetupScreen(
                userProfileStore = userProfileStore,
                onBack = { nav.popBackStack() },
                onGoMasterVault = { nav.navigate(Routes.MASTER_VAULT) } // ✅ Added this line!
            )
        }
        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }
        composable(Routes.MASTER_VAULT) {
            com.rehanu04.resumematchv2.ui.MasterVaultScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore
            )
        }
    }
}