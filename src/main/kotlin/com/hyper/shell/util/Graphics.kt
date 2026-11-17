package com.hyper.shell.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

fun Drawable.toImageBitmap(size: Int = 160): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = bounds
    setBounds(0, 0, size, size)
    draw(canvas)
    bounds = oldBounds
    return bitmap.asImageBitmap()
}

/**
 * Application icons are identical for every activity of a package, so cache them
 * once. Everything is decoded on a background dispatcher by the caller.
 */
object IconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val appIcons = ConcurrentHashMap<String, ImageBitmap>()

    fun appIcon(context: Context, packageName: String, size: Int = 160): ImageBitmap? {
        cache[packageName]?.let { return it }
        return runCatching {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            drawable.toImageBitmap(size).also { cache[packageName] = it }
        }.getOrNull()
    }

    fun iconFromDrawable(key: String, drawable: Drawable, size: Int = 160): ImageBitmap {
        appIcons[key]?.let { return it }
        val bitmap = drawable.toImageBitmap(size)
        appIcons[key] = bitmap
        return bitmap
    }

    fun clear() {
        cache.clear()
        appIcons.clear()
    }

    @Suppress("unused")
    fun packageManager(context: Context): PackageManager = context.packageManager
}
