package com.whattocook.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whattocook.app.ui.account.AccountScreen
import com.whattocook.app.ui.account.AuthScreen
import com.whattocook.app.ui.discover.DiscoverScreen
import com.whattocook.app.ui.home.HomeScreen
import com.whattocook.app.ui.importer.ImportScreen
import com.whattocook.app.ui.library.LibraryListScreen
import com.whattocook.app.ui.onboarding.OnboardingScreen
import com.whattocook.app.ui.pantry.PantryScreen
import com.whattocook.app.ui.paywall.PaywallScreen
import com.whattocook.app.ui.recipe.CookModeScreen
import com.whattocook.app.ui.recipe.GenerateScreen
import com.whattocook.app.ui.recipe.RecipeDetailScreen
import com.whattocook.app.ui.shopping.ShoppingScreen
import com.whattocook.app.ui.theme.AppTheme
import com.whattocook.app.ui.theme.Motion

// Material "fade through" between tabs: the old screen fades out quickly, then the new one
// fades in while growing slightly. Pushed screens use a short shared-axis slide.
private const val FADE_OUT_MS = 90
private const val FADE_IN_MS = 210

private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing)) +
        scaleIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = Motion.EmphasizedDecelerate), initialScale = 0.97f)
}
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(FADE_OUT_MS, easing = FastOutLinearInEasing))
}
private val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        tween(Motion.MEDIUM, easing = Motion.Emphasized),
        initialOffset = { it / 5 },
    ) + fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearOutSlowInEasing))
}
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        tween(Motion.MEDIUM, easing = Motion.Emphasized),
        targetOffset = { it / 5 },
    ) + fadeOut(tween(FADE_OUT_MS * 2, easing = FastOutLinearInEasing))
}
private val sheetEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Up,
        tween(Motion.LONG, easing = Motion.EmphasizedDecelerate),
        initialOffset = { it / 3 },
    ) + fadeIn(tween(FADE_IN_MS, easing = LinearOutSlowInEasing))
}
private val sheetExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Down,
        tween(250, easing = Motion.EmphasizedAccelerate),
        targetOffset = { it / 3 },
    ) + fadeOut(tween(200, easing = FastOutLinearInEasing))
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Main app shell: bottom navigation plus the navigation graph. */
@Composable
fun AppRoot(
    sharedLink: String? = null,
    onSharedLinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    // A reel shared from Instagram / TikTok / YouTube opens the import screen.
    LaunchedEffect(sharedLink) {
        if (sharedLink != null) {
            navController.navigate(Routes.import(sharedLink)) { launchSingleTop = true }
            onSharedLinkHandled()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it } +
                    fadeIn(tween(Motion.MEDIUM)),
                exit = slideOutVertically(tween(200, easing = Motion.EmphasizedAccelerate)) { it } +
                    fadeOut(tween(200)),
            ) {
                BottomBar(
                    current = currentTab,
                    onSelect = { navController.navigateToTab(it.route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabEnter,
            popExitTransition = tabExit,
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
                    onOpenPantry = { navController.navigateToTab(Routes.PANTRY) },
                    onFindRecipes = { navController.navigate(Routes.generate()) },
                    onImport = { navController.navigate(Routes.import()) },
                    onOpenDiscover = { navController.navigateToTab(Routes.DISCOVER) },
                )
            }
            composable(Routes.PANTRY) {
                PantryScreen(onFindRecipes = { navController.navigate(Routes.generate()) })
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    onStartRequest = { route -> navController.navigate(route) },
                    onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
                    onOpenLibrary = { source, title -> navController.navigate(Routes.library(source, title)) },
                    onLockedDish = { navController.navigate(Routes.paywall(reason = Routes.REASON_LIBRARY)) },
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen()
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onOpenPlan = { navController.navigate(Routes.paywall()) },
                    onViewOnboarding = { navController.navigate(Routes.ONBOARDING) },
                    onOpenAuth = { navController.navigate(Routes.AUTH) },
                )
            }
            composable(
                route = Routes.AUTH,
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                AuthScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.GENERATE,
                arguments = listOf(
                    navArgument(Routes.ARG_MOOD) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Routes.ARG_CUISINE) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Routes.ARG_SURPRISE) { type = NavType.BoolType; defaultValue = false },
                    navArgument(Routes.ARG_USE_PANTRY) { type = NavType.BoolType; defaultValue = true },
                ),
                enterTransition = pushEnter,
                popExitTransition = popExit,
            ) {
                GenerateScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
                    onOpenPantry = {
                        navController.popBackStack()
                        navController.navigateToTab(Routes.PANTRY)
                    },
                    onLimitReached = { resetsAt ->
                        navController.navigate(Routes.paywall(resetsAt)) {
                            popUpTo(Routes.GENERATE) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.LIBRARY,
                arguments = listOf(
                    navArgument(Routes.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Routes.ARG_VALUE) { type = NavType.StringType },
                    navArgument(Routes.ARG_TITLE) { type = NavType.StringType; defaultValue = "" },
                ),
                enterTransition = pushEnter,
                popExitTransition = popExit,
            ) {
                LibraryListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
                    onLockedDish = { navController.navigate(Routes.paywall(reason = Routes.REASON_LIBRARY)) },
                )
            }
            composable(
                route = Routes.IMPORT,
                arguments = listOf(
                    navArgument(Routes.ARG_URL) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                ImportScreen(
                    onBack = { navController.popBackStack() },
                    onRecipeReady = { id ->
                        navController.navigate(Routes.recipe(id)) {
                            popUpTo(Routes.IMPORT) { inclusive = true }
                        }
                    },
                    onFindRecipes = {
                        navController.navigate(Routes.generate()) {
                            popUpTo(Routes.IMPORT) { inclusive = true }
                        }
                    },
                    onLimitReached = { resetsAt ->
                        navController.navigate(Routes.paywall(resetsAt)) {
                            popUpTo(Routes.IMPORT) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.RECIPE,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
                enterTransition = pushEnter,
                popExitTransition = popExit,
            ) {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onCookMode = { navController.navigate(Routes.cook(it)) },
                )
            }
            composable(
                route = Routes.COOK,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                CookModeScreen(onClose = { navController.popBackStack() })
            }
            composable(
                route = Routes.PAYWALL,
                arguments = listOf(
                    navArgument(Routes.ARG_RESETS_AT) { type = NavType.LongType; defaultValue = 0L },
                    navArgument(Routes.ARG_REASON) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                PaywallScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinished = { navController.popBackStack() })
            }
        }
    }
}
