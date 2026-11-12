package com.aura.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aura.launcher.ui.HomeScreen
import com.aura.launcher.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuraTheme {
                val apps by viewModel.apps.collectAsState()
                val loaded by viewModel.loaded.collectAsState()
                val watch by viewModel.watch.collectAsState()
                val watchEnabled by viewModel.watchEnabled.collectAsState()

                HomeScreen(
                    apps = apps,
                    loaded = loaded,
                    watch = watch,
                    watchEnabled = watchEnabled,
                    onLaunch = { app ->
                        runCatching { startActivity(viewModel.repository.launchIntent(app)) }
                            .onFailure {
                                Toast.makeText(this, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onLaunchWatch = { entry ->
                        val intent = viewModel.repository.launchIntentForPackage(entry.packageName)
                        runCatching { startActivity(requireNotNull(intent)) }
                            .onFailure {
                                Toast.makeText(this, "No se pudo abrir ${entry.appLabel}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onForgetWatch = { entry -> viewModel.forget(entry.packageName) },
                    onEnableWatch = {
                        runCatching {
                            startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure {
                            // Muchas teles no incluyen esta pantalla: se concede por ADB (ver README).
                            Toast.makeText(this, "Actívalo por ADB: mira el README", Toast.LENGTH_LONG).show()
                        }
                    },
                    onOpenSettings = {
                        runCatching {
                            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
        viewModel.refresh()
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    // Un launcher es la raíz: el botón Atrás no debe cerrarlo.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intencionadamente vacío.
    }
}
