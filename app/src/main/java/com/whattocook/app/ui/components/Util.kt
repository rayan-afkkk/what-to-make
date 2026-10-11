package com.whattocook.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.whattocook.app.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Plural string lookup that works on every Compose version. */
@Composable
@ReadOnlyComposable
fun quantityString(@PluralsRes id: Int, count: Int, vararg args: Any): String {
    LocalConfiguration.current // recompose on configuration change
    return LocalContext.current.resources.getQuantityString(id, count, *args)
}

/**
 * True when the user turned animations off (Developer options or the accessibility
 * "Remove animations" setting, both of which set the animator duration scale to 0).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** "Today", "Yesterday", "3 days ago" or a short date. */
@Composable
fun relativeDate(timeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val days = daysBetween(timeMillis, nowMillis)
    return when {
        days <= 0 -> stringResource(R.string.date_today)
        days == 1 -> stringResource(R.string.date_yesterday)
        days < 7 -> quantityString(R.plurals.date_days_ago, days, days)
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timeMillis))
    }
}

/** Calendar days between two instants in the device time zone. */
fun daysBetween(fromMillis: Long, toMillis: Long): Int {
    fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((dayStart(toMillis) - dayStart(fromMillis)) / (24L * 60 * 60 * 1000)).toInt()
}

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
