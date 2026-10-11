package com.whattocook.app.ui.discover

import androidx.annotation.StringRes
import com.whattocook.app.R
import com.whattocook.app.domain.BrowseSource
import com.whattocook.app.domain.BrowseSource.Type.AREA
import com.whattocook.app.domain.BrowseSource.Type.CATEGORY

/** One horizontal row of library dishes on Discover. */
data class LibraryRow(
    val key: String,
    @StringRes val title: Int,
    val source: BrowseSource,
)

/** Values are TheMealDB category / area names. Pork is intentionally not offered. */
val LibraryRows = listOf(
    LibraryRow("desi", R.string.library_desi, BrowseSource(AREA, "Indian")),
    LibraryRow("chicken", R.string.library_chicken, BrowseSource(CATEGORY, "Chicken")),
    LibraryRow("beef", R.string.library_beef, BrowseSource(CATEGORY, "Beef")),
    LibraryRow("seafood", R.string.library_seafood, BrowseSource(CATEGORY, "Seafood")),
    LibraryRow("vegetarian", R.string.library_vegetarian, BrowseSource(CATEGORY, "Vegetarian")),
    LibraryRow("pasta", R.string.library_pasta, BrowseSource(CATEGORY, "Pasta")),
    LibraryRow("breakfast", R.string.library_breakfast, BrowseSource(CATEGORY, "Breakfast")),
    LibraryRow("dessert", R.string.library_dessert, BrowseSource(CATEGORY, "Dessert")),
    LibraryRow("lamb", R.string.library_lamb, BrowseSource(CATEGORY, "Lamb")),
    LibraryRow("italian", R.string.library_italian, BrowseSource(AREA, "Italian")),
    LibraryRow("chinese", R.string.library_chinese, BrowseSource(AREA, "Chinese")),
    LibraryRow("turkish", R.string.library_turkish, BrowseSource(AREA, "Turkish")),
    LibraryRow("mexican", R.string.library_mexican, BrowseSource(AREA, "Mexican")),
    LibraryRow("thai", R.string.library_thai, BrowseSource(AREA, "Thai")),
    LibraryRow("japanese", R.string.library_japanese, BrowseSource(AREA, "Japanese")),
    LibraryRow("moroccan", R.string.library_moroccan, BrowseSource(AREA, "Moroccan")),
    LibraryRow("greek", R.string.library_greek, BrowseSource(AREA, "Greek")),
    LibraryRow("french", R.string.library_french, BrowseSource(AREA, "French")),
    LibraryRow("vegan", R.string.library_vegan, BrowseSource(CATEGORY, "Vegan")),
    LibraryRow("starter", R.string.library_starters, BrowseSource(CATEGORY, "Starter")),
    LibraryRow("side", R.string.library_sides, BrowseSource(CATEGORY, "Side")),
    LibraryRow("goat", R.string.library_goat, BrowseSource(CATEGORY, "Goat")),
)
