package com.whattocook.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whattocook.app.ui.AppViewModel
import com.whattocook.app.ui.navigation.AppRoot
import com.whattocook.app.ui.onboarding.OnboardingScreen
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.MAX_FONT_SCALE
import com.whattocook.app.ui.theme.WhatToCookTheme
import com.whattocook.app.ui.theme.isAppInDarkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    /**
     * Caps the system font size so text, cards and buttons keep the same proportions even
     * when the phone's font size is maxed out. Applied to the activity context so dialogs and
     * bottom sheets (which live in their own windows) follow it too.
     */
    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        if (configuration.fontScale > MAX_FONT_SCALE) {
            val capped = Configuration(configuration).apply { fontScale = MAX_FONT_SCALE }
            super.attachBaseContext(newBase.createConfigurationContext(capped))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = isAppInDarkTheme(state.themeMode)

            // Keep status/navigation bar icons readable when the in-app theme changes.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            WhatToCookTheme(darkTheme = darkTheme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                ) {
                    AnimatedContent(
                        targetState = state.onboardingCompleted,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "root",
                    ) { completed ->
                        when (completed) {
                            null -> Box(Modifier.fillMaxSize()) // DataStore still loading
                            false -> OnboardingScreen(onFinished = appViewModel::completeOnboarding)
                            true -> AppRoot()
                        }
                    }
                }
            }
        }
    }
}
