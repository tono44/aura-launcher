package com.aura.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.aura.launcher.R
import com.aura.launcher.data.AppEntry
import com.aura.launcher.data.WatchEntry
import com.aura.launcher.ui.theme.AuraColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Medidas en dp. Si algo se ve apretado en tu tele, empieza por ajustar estas constantes.
private val AppCardWidth = 232.dp
private val AppCardWidthCompact = 184.dp // cuando hay fila de "Seguir viendo"
private val WatchCardWidth = 208.dp
private val CardShape = RoundedCornerShape(18.dp)
private val Inset = 76.dp

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    loaded: Boolean,
    watch: List<WatchEntry>,
    watchEnabled: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLaunchWatch: (WatchEntry) -> Unit,
    onForgetWatch: (WatchEntry) -> Unit,
    onEnableWatch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var focusedLabel by remember { mutableStateOf("") }
    val firstWatch = remember { FocusRequester() }
    val firstApp = remember { FocusRequester() }
    val enableHint = stringResource(R.string.watch_enable_hint)

    // La fila aparece si hay historial, o para invitar a activarla si aún no hay permiso.
    // Con el permiso concedido y sin nada visto, se oculta y la pantalla queda limpia.
    val showWatch = watch.isNotEmpty() || !watchEnabled

    LaunchedEffect(watch.isNotEmpty(), apps.isNotEmpty()) {
        withFrameNanos { } // espera a que las filas estén medidas
        runCatching {
            // El foco inicial va a las apps; "Seguir viendo" queda a un paso hacia abajo.
            when {
                apps.isNotEmpty() -> firstApp.requestFocus()
                watch.isNotEmpty() -> firstWatch.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AuraColors.BackgroundLift, AuraColors.Background),
                )
            )
            .padding(vertical = if (showWatch) 40.dp else 56.dp),
    ) {
        Clock(
            compact = showWatch,
            modifier = Modifier.align(Alignment.TopStart).padding(start = Inset),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
            if (loaded && apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_apps),
                    modifier = Modifier.padding(start = Inset),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuraColors.TextSecondary,
                )
            }

            val appCardWidth = if (showWatch) AppCardWidthCompact else AppCardWidth

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                // Margen para que el escalado del foco no se recorte.
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = Inset),
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    AppCard(
                        app = app,
                        cardWidth = appCardWidth,
                        modifier = if (index == 0) Modifier.focusRequester(firstApp) else Modifier,
                        onFocused = { focusedLabel = app.label },
                        onClick = { onLaunch(app) },
                    )
                }
                item(key = "settings") {
                    SettingsCard(
                        cardWidth = appCardWidth,
                        onFocused = { focusedLabel = "" },
                        onClick = onOpenSettings,
                    )
                }
            }

            // "Seguir viendo" va debajo de las apps.
            if (showWatch) {
                SectionLabel(stringResource(R.string.watch_title))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = Inset),
                ) {
                    itemsIndexed(watch, key = { _, e -> e.packageName }) { index, entry ->
                        WatchCard(
                            entry = entry,
                            modifier = if (index == 0) Modifier.focusRequester(firstWatch) else Modifier,
                            onFocused = { focusedLabel = "${entry.title} · ${entry.appLabel}" },
                            onClick = { onLaunchWatch(entry) },
                            onLongClick = { onForgetWatch(entry) },
                        )
                    }
                    if (!watchEnabled) {
                        item(key = "enable-watch") {
                            EnableWatchCard(
                                onFocused = { focusedLabel = enableHint },
                                onClick = onEnableWatch,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nombre de lo enfocado, con una transición suave.
            Crossfade(targetState = focusedLabel, label = "focused-label") { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Inset, end = Inset).height(36.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AuraColors.TextSecondary,
        modifier = Modifier.padding(start = Inset, top = 4.dp),
    )
}

@Composable
private fun Clock(compact: Boolean, modifier: Modifier = Modifier) {
    // java.time exige Android 8; SimpleDateFormat funciona en todas las versiones.
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(10_000)
        }
    }
    val locale = Locale.getDefault()
    val time = SimpleDateFormat("HH:mm", locale).format(now)
    val date = SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(now)
        .replaceFirstChar { it.titlecase(locale) }

    Column(modifier) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge.let {
                if (compact) it.copy(fontSize = 56.sp) else it
            },
            color = AuraColors.TextPrimary,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge,
            color = AuraColors.TextSecondary,
        )
    }
}

@Composable
private fun WatchCard(
    entry: WatchEntry,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        width = WatchCardWidth,
        modifier = modifier,
        onFocused = onFocused,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            val art = entry.artwork
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AuraColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Barra de progreso fina, solo si el contenido informa de su duración.
            entry.progress?.let { progress ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(AuraColors.Accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnableWatchCard(
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusableCard(width = WatchCardWidth, onFocused = onFocused, onClick = onClick) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.watch_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = AuraColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    cardWidth: Dp,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        width = cardWidth,
        modifier = modifier,
        onFocused = onFocused,
        onClick = onClick,
    ) {
        val bitmap = app.banner
        if (bitmap != null) {
            if (app.hasBanner) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Sin banner: icono centrado sobre el fondo de la tarjeta.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(cardWidth * 0.3f),
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    cardWidth: Dp,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusableCard(width = cardWidth, onFocused = onFocused, onClick = onClick) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = AuraColors.TextSecondary,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusableCard(
    width: Dp,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .onFocusChanged { if (it.hasFocus) onFocused() },
        shape = ClickableSurfaceDefaults.shape(shape = CardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AuraColors.Card,
            focusedContainerColor = AuraColors.CardFocused,
            pressedContainerColor = AuraColors.CardFocused,
            contentColor = AuraColors.TextPrimary,
            focusedContentColor = AuraColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                shape = CardShape,
            ),
        ),
    ) {
        Box(Modifier.fillMaxSize().clip(CardShape)) { content() }
    }
}
