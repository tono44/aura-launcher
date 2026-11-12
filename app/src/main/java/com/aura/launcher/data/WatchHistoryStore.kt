package com.aura.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Historial local de "Seguir viendo". Una entrada por app, la más reciente primero.
 * Se guarda en SharedPreferences (JSON) y las carátulas en ficheros PNG privados.
 */
class WatchHistoryStore private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("watch_history", Context.MODE_PRIVATE)
    private val artDir = File(context.filesDir, "watch").apply { mkdirs() }

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<WatchEntry>> = _entries.asStateFlow()

    @Synchronized
    fun record(
        packageName: String,
        appLabel: String,
        title: String,
        subtitle: String?,
        progress: Float?,
        artwork: Bitmap?,
    ) {
        if (artwork != null) saveArtwork(packageName, artwork)

        val now = System.currentTimeMillis()
        val stored = readRaw().filterNot { it.getString("pkg") == packageName }.toMutableList()
        stored.add(
            0,
            JSONObject()
                .put("pkg", packageName)
                .put("app", appLabel)
                .put("title", title)
                .put("subtitle", subtitle ?: JSONObject.NULL)
                .put("progress", progress?.toDouble() ?: JSONObject.NULL)
                .put("at", now),
        )
        val trimmed = stored.take(MAX_ENTRIES)
        // Borra las carátulas de las entradas que han salido del límite.
        stored.drop(MAX_ENTRIES).forEach { File(artDir, it.getString("pkg") + ".png").delete() }

        prefs.edit().putString(KEY, JSONArray(trimmed).toString()).apply()
        _entries.value = load()
    }

    @Synchronized
    fun remove(packageName: String) {
        val kept = readRaw().filterNot { it.getString("pkg") == packageName }
        File(artDir, "$packageName.png").delete()
        prefs.edit().putString(KEY, JSONArray(kept).toString()).apply()
        _entries.value = load()
    }

    private fun readRaw(): List<JSONObject> {
        val text = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(text)
            (0 until array.length()).map { array.getJSONObject(it) }
        }.getOrDefault(emptyList())
    }

    private fun load(): List<WatchEntry> = readRaw().map { o ->
        val pkg = o.getString("pkg")
        val art = File(artDir, "$pkg.png")
            .takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        WatchEntry(
            packageName = pkg,
            appLabel = o.optString("app"),
            title = o.optString("title"),
            subtitle = if (o.isNull("subtitle")) null else o.optString("subtitle"),
            progress = if (o.isNull("progress")) null else o.optDouble("progress").toFloat(),
            updatedAt = o.optLong("at"),
            artwork = art,
        )
    }

    private fun saveArtwork(packageName: String, bitmap: Bitmap) {
        runCatching {
            // Reducida a 16:9 de 640 px de ancho: suficiente para una tarjeta de TV.
            val targetW = 640
            val targetH = 360
            val scale = maxOf(targetW / bitmap.width.toFloat(), targetH / bitmap.height.toFloat())
            val w = (bitmap.width * scale).toInt()
            val h = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
            val x = ((w - targetW) / 2).coerceAtLeast(0)
            val y = ((h - targetH) / 2).coerceAtLeast(0)
            val cropped = Bitmap.createBitmap(scaled, x, y, minOf(targetW, w), minOf(targetH, h))
            File(artDir, "$packageName.png").outputStream().use {
                cropped.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        private const val KEY = "entries"
        private const val MAX_ENTRIES = 12

        @Volatile
        private var instance: WatchHistoryStore? = null

        fun get(context: Context): WatchHistoryStore =
            instance ?: synchronized(this) {
                instance ?: WatchHistoryStore(context.applicationContext).also { instance = it }
            }
    }
}
