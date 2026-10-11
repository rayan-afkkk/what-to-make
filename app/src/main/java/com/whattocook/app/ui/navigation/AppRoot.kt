package com.whattocook.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.whattocook.app.ui.discover.DiscoverScreen
import com.whattocook.app.ui.home.HomeScreen
import com.whattocook.app.ui.onboarding.OnboardingScreen
import com.whattocook.app.ui.pantry.PantryScreen
import com.whattocook.app.ui.paywall.PaywallScreen
import com.whattocook.app.ui.recipe.CookModeScreen
import com.whattocook.app.ui.recipe.GenerateScreen
import com.whattocook.app.ui.recipe.RecipeDetailScreen
import com.whattocook.app.ui.shopping.ShoppingScreen
import com.whattocook.app.ui.theme.AppTheme

private const val FADE_MS = 220
private const val SLIDE_MS = 320

private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(tween(FADE_MS)) }
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(tween(FADE_MS)) }
private val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) + fadeIn(tween(SLIDE_MS)) }
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) + fadeOut(tween(SLIDE_MS)) }
private val sheetEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(SLIDE_MS)) + fadeIn(tween(SLIDE_MS)) }
private val sheetExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(SLIDE_MS)) + fadeOut(tween(SLIDE_MS)) }

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Main app shell: bottom navigation plus the navigation graph. */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
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
                )
            }
            composable(Routes.PANTRY) {
                PantryScreen(onFindRecipes = { navController.navigate(Routes.generate()) })
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(onStartRequest = { route -> navController.navigate(route) })
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen()
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onOpenPlan = { navController.navigate(Routes.paywall()) },
                    onViewOnboarding = { navController.navigate(Routes.ONBOARDING) },
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
                arguments = listOf(navArgument(Routes.ARG_RESETS_AT) { type = NavType.LongType; defaultValue = 0L }),
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                PaywallScreen(onClose = { navController.popBackStack() })
            }
            composable(
                route = Routes.ONBOARDING,
                enterTransition = sheetEnter,
                popExitTransition = sheetExit,
            ) {
                OnboardingScreen(onFinished = { navController.popBackStack() })
            }
        }
    }
}
