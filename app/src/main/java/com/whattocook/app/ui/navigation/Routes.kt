package com.whattocook.app.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.whattocook.app.R
import com.whattocook.app.domain.BrowseSource

object Routes {
    const val HOME = "home"
    const val PANTRY = "pantry"
    const val DISCOVER = "discover"
    const val SHOPPING = "shopping"
    const val ACCOUNT = "account"
    const val ONBOARDING = "onboarding"

    const val ARG_ID = "id"
    const val RECIPE = "recipe/{$ARG_ID}"
    const val COOK = "cook/{$ARG_ID}"

    const val ARG_MOOD = "mood"
    const val ARG_CUISINE = "cuisine"
    const val ARG_SURPRISE = "surprise"
    const val ARG_USE_PANTRY = "usePantry"
    const val GENERATE =
        "generate?$ARG_MOOD={$ARG_MOOD}&$ARG_CUISINE={$ARG_CUISINE}&$ARG_SURPRISE={$ARG_SURPRISE}&$ARG_USE_PANTRY={$ARG_USE_PANTRY}"

    const val ARG_TYPE = "type"
    const val ARG_VALUE = "value"
    const val ARG_TITLE = "title"
    const val LIBRARY = "library/{$ARG_TYPE}/{$ARG_VALUE}?$ARG_TITLE={$ARG_TITLE}"

    const val ARG_URL = "url"
    const val IMPORT = "import?$ARG_URL={$ARG_URL}"

    const val ARG_RESETS_AT = "resetsAt"
    const val PAYWALL = "paywall?$ARG_RESETS_AT={$ARG_RESETS_AT}"

    fun recipe(id: String) = "recipe/${Uri.encode(id)}"
    fun cook(id: String) = "cook/${Uri.encode(id)}"

    fun generate(
        mood: String? = null,
        cuisine: String? = null,
        surprise: Boolean = false,
        usePantry: Boolean = true,
    ): String = buildString {
        append("generate?$ARG_SURPRISE=$surprise&$ARG_USE_PANTRY=$usePantry")
        if (mood != null) append("&$ARG_MOOD=${Uri.encode(mood)}")
        if (cuisine != null) append("&$ARG_CUISINE=${Uri.encode(cuisine)}")
    }

    fun paywall(resetsAtMillis: Long = 0L) = "paywall?$ARG_RESETS_AT=$resetsAtMillis"

    fun library(source: BrowseSource, title: String) =
        "library/${source.type.name}/${Uri.encode(source.value)}?$ARG_TITLE=${Uri.encode(title)}"

    fun import(url: String? = null) =
        if (url.isNullOrBlank()) "import" else "import?$ARG_URL=${Uri.encode(url)}"
}

enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    PANTRY(Routes.PANTRY, R.string.tab_pantry, Icons.Filled.Kitchen, Icons.Outlined.Kitchen),
    DISCOVER(Routes.DISCOVER, R.string.tab_discover, Icons.Filled.Explore, Icons.Outlined.Explore),
    SHOPPING(Routes.SHOPPING, R.string.tab_shopping, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    ACCOUNT(Routes.ACCOUNT, R.string.tab_account, Icons.Filled.Person, Icons.Outlined.Person),
}
