package com.aura.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val leanback = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val leanbackPackages = leanback.map { it.activityInfo.packageName }.toSet()

        // Apps pensadas para móvil que no tienen entrada Leanback: se muestran igualmente
        // para no dejar fuera ninguna app instalada desde fuera de la Play Store de TV.
        val others = query(Intent.CATEGORY_LAUNCHER)
            .filter { it.activityInfo.packageName !in leanbackPackages }

        (leanback + others)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { it.toEntry() }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchIntent(app: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    /** Intent para abrir una app por su paquete (usado por "Seguir viendo"). */
    fun launchIntentForPackage(packageName: String): Intent? =
        (pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return pm.queryIntentActivities(intent, 0)
    }

    private fun ResolveInfo.toEntry(): AppEntry {
        val info = activityInfo
        val banner = runCatching { info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm) }.getOrNull()
        val visual = banner ?: runCatching { loadIcon(pm) }.getOrNull()
        val bitmap = visual?.let {
            runCatching { it.toBitmap(width = 320, height = if (banner != null) 180 else 320).asImageBitmap() }
                .getOrNull()
        }
        return AppEntry(
            label = loadLabel(pm).toString(),
            packageName = info.packageName,
            component = ComponentName(info.packageName, info.name),
            banner = bitmap,
            hasBanner = banner != null,
        )
    }
}
