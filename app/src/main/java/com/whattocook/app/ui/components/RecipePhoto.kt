package com.whattocook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whattocook.app.ui.theme.AppTheme

/** A dish photo that fades in once loaded, with a soft placeholder while loading or offline. */
@Composable
fun RecipePhoto(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    thumbnail: Boolean = false,
) {
    val context = LocalContext.current
    val placeholder = ColorPainter(AppTheme.colors.cardRaised)
    val request = remember(url, thumbnail) {
        ImageRequest.Builder(context)
            .data(if (thumbnail) thumbnailUrl(url) else url)
            .crossfade(250)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(AppTheme.colors.cardRaised),
    )
}

/** TheMealDB serves a small (~250px) version of each photo at "<url>/preview". */
private fun thumbnailUrl(url: String): String =
    if (url.contains("themealdb.com/images/media/meals/") && !url.endsWith("/preview")) "$url/preview" else url
