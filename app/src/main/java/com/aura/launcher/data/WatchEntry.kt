package com.aura.launcher.data

import androidx.compose.ui.graphics.ImageBitmap

/** Lo último que se ha visto en una app de streaming (una entrada por app). */
data class WatchEntry(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val subtitle: String?,
    /** Progreso entre 0 y 1, o null si el contenido no informa de duración (directos). */
    val progress: Float?,
    val updatedAt: Long,
    val artwork: ImageBitmap?,
)
