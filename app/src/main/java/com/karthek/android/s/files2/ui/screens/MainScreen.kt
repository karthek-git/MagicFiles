package com.karthek.android.s.files2.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.karthek.android.s.files2.LicensesContent
import com.karthek.android.s.files2.SettingsScreen
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.ui.components.loadInterstitialAd
import com.karthek.android.s.files2.ui.screens.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(handleFile: (SFile) -> Unit) {
    val backStack = rememberNavBackStack(Screen.Home)
    val onBackClick = { backStack.removeLastOrNull(); Unit }

    val context = LocalContext.current
    var interstitialAd: InterstitialAd? = null
    val interstitialAdLoader = {
        loadInterstitialAd(
            context,
            adLoadedCallback = { interstitialAd = it },
            adCompleteCallback = { interstitialAd = null }
        )
    }
    val activity = LocalActivity.current
    var adTimer = false
    val backgroundScope = rememberCoroutineScope()
    val showInterstitialAd = {
        interstitialAd?.let {
            if (adTimer) {
                it.show(activity!!)
                adTimer = false
            }
        } ?: interstitialAdLoader()
    }
    LaunchedEffect(context) {
        interstitialAdLoader()
        backgroundScope.launch(Dispatchers.Default) {
            while (true) {
                delay(60000)
                adTimer = true
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<Screen.Home> {
                FileListScreen(
                    handleFile = handleFile,
                    onMoreClick = {
                        backStack.add(Screen.Settings)
                        showInterstitialAd()
                    },
                    showInterstitialAd = showInterstitialAd
                )
            }
            entry<Screen.Settings> {
                SettingsScreen(
                    onBackClick = onBackClick,
                    onLicensesClick = { backStack.add(Screen.Licenses) })
            }
            entry<Screen.Licenses> { LicensesContent(onBackClick = onBackClick) }
        }
    )
}

