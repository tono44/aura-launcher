package com.aura.launcher.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.aura.launcher.data.WatchHistoryStore

/**
 * Observa las sesiones multimedia activas (Netflix, YouTube, Prime Video…) y guarda
 * lo que se está viendo. Android solo deja leer sesiones de otras apps a un
 * NotificationListenerService con acceso concedido por el usuario.
 */
class MediaWatchService : NotificationListenerService() {

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var store: WatchHistoryStore
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attach(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        store = WatchHistoryStore.get(this)
        val component = ComponentName(this, MediaWatchService::class.java)
        runCatching {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component)
            attach(sessionManager.getActiveSessions(component))
        }
    }

    override fun onListenerDisconnected() {
        detachAll()
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        super.onListenerDisconnected()
    }

    private fun attach(controllers: List<MediaController>) {
        detachAll()
        controllers
            .filter { it.packageName !in IGNORED_PACKAGES && it.packageName != packageName }
            .forEach { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) = capture(controller)
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        // Se guarda al empezar, al pausar y al parar: ahí la posición es fiable.
                        if (state?.state in CAPTURE_STATES) capture(controller)
                    }
                }
                controller.registerCallback(callback)
                callbacks[controller] = callback
                capture(controller)
            }
    }

    private fun detachAll() {
        callbacks.forEach { (controller, callback) -> runCatching { controller.unregisterCallback(callback) } }
        callbacks.clear()
    }

    private fun capture(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val state = controller.playbackState ?: return
        if (state.state !in CAPTURE_STATES) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: return
        val subtitle = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)

        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val progress = if (duration > 0 && state.position >= 0) {
            (state.position.toFloat() / duration).coerceIn(0f, 1f)
        } else null

        // Si ya se terminó (más del 95 %), no tiene sentido ofrecer "seguir viendo".
        if (progress != null && progress > 0.95f) {
            store.remove(controller.packageName)
            return
        }

        val artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        store.record(
            packageName = controller.packageName,
            appLabel = labelOf(controller.packageName),
            title = title,
            subtitle = subtitle,
            progress = progress,
            artwork = artwork,
        )
    }

    private fun labelOf(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(pkg)

    companion object {
        private val CAPTURE_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED,
        )

        /** Apps de solo audio: no son "seguir viendo". Amplía la lista a tu gusto. */
        private val IGNORED_PACKAGES = setOf(
            "com.spotify.tv.android",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube.tvmusic",
            "com.amazon.mp3",
            "deezer.android.tv",
            "deezer.android.app",
            "com.apple.android.music",
            "com.tidal.android",
        )

        fun isEnabled(context: Context): Boolean =
            androidx.core.app.NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)
    }
}
