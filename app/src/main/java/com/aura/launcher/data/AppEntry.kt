package com.aura.launcher.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

/** Una app instalada que se puede lanzar desde Aura. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    /** Banner (16:9) si la app lo define, o icono como alternativa. */
    val banner: ImageBitmap?,
    val hasBanner: Boolean,
)
