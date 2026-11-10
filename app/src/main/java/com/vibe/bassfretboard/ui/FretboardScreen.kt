package com.vibe.bassfretboard.ui

import androidx.compose.animation.core.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.bassfretboard.model.FretPosition
import com.vibe.bassfretboard.model.FretboardGeometry
import com.vibe.bassfretboard.model.Notes
import com.vibe.bassfretboard.model.PointerNoteMapper
import com.vibe.bassfretboard.model.Tuning
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun FretboardScreen(viewModel: FretboardViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pressed by viewModel.pressedNotes.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Bass Fretboard") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        FretboardCanvas(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            settings = settings,
            pressed = pressed,
            onPointerDown = viewModel::onPointerDown,
            onPointerMove = viewModel::onPointerMove,
            onPointerUp = viewModel::onPointerUp,
        )
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onTuningSelected = viewModel::setTuningId,
            onFretCountChanged = viewModel::setFretCount,
            onVerticalChanged = viewModel::setVertical,
            onLeftHandedChanged = viewModel::setLeftHanded,
            onNoteNamesChanged = viewModel::setShowNoteNames,
        )
    }
}

@Composable
private fun FretboardCanvas(
    modifier: Modifier,
    settings: AppSettings,
    pressed: Map<Long, PressedNote>,
    onPointerDown: (Long, FretPosition) -> Unit,
    onPointerMove: (Long, FretPosition) -> Unit,
    onPointerUp: (Long) -> Unit,
) {
    var nowNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            nowNanos = withFrameNanos { it }
        }
    }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val tuning = Tuning.byId(settings.tuningId)
        val layout = remember(widthPx, heightPx, settings.vertical, settings.leftHanded, settings.fretCount, tuning.stringCount) {
            FretboardLayout(
                widthPx,
                heightPx,
                settings.vertical,
                settings.leftHanded,
                settings.fretCount,
                tuning.stringCount,
            )
        }
        val mapper = remember(tuning) { PointerNoteMapper(tuning) }
        val activePointers = remember(settings, widthPx, heightPx) { mutableMapOf<Long, Offset>() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings, widthPx, heightPx) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                val id = change.id.value
                                if (change.pressed) {
                                    val (u, v) = layout.toLogical(change.position)
                                    val pos = mapper.map(u, v, layout.geometry, layout.stringCoords, layout.stringHitRadius)
                                    if (pos != null) {
                                        if (!activePointers.containsKey(id)) {
                                            onPointerDown(id, pos)
                                        } else if (activePointers[id] != change.position) {
                                            onPointerMove(id, pos)
                                        }
                                    }
                                    activePointers[id] = change.position
                                    change.consume()
                                } else {
                                    if (activePointers.remove(id) != null) {
                                        onPointerUp(id)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                },
        ) {
            drawFretboard(layout, settings, pressed, nowNanos)
        }
    }
}

private val markerFrets = setOf(3, 5, 7, 9, 15, 17, 19, 21)
private val doubleMarkerFrets = setOf(12, 24)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFretboard(
    layout: FretboardLayout,
    settings: AppSettings,
    pressed: Map<Long, PressedNote>,
    nowNanos: Long,
) {
    val woodDark = Color(0xFF2A1B10)
    val woodLight = Color(0xFF4A2F1B)
    val fretColor = Color(0xFFC0C0C8)
    val markerColor = Color(0xFFE8D9B0)
    val accent = Color(0xFFFFB300)

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(woodDark, woodLight, woodDark),
            start = Offset(0f, 0f),
            end = if (layout.vertical) Offset(0f, size.height) else Offset(size.width, 0f),
        ),
    )

    val neckTopLeft: Offset
    val neckSize: Size
    if (layout.vertical) {
        neckTopLeft = Offset(layout.stringCoords.first() - 40f, layout.nutU - 30f)
        neckSize = Size(
            layout.stringCoords.last() - layout.stringCoords.first() + 80f,
            layout.bridgeU - layout.nutU + 60f,
        )
    } else {
        neckTopLeft = Offset(layout.nutU - 30f, layout.stringCoords.first() - 40f)
        neckSize = Size(
            layout.bridgeU - layout.nutU + 60f,
            layout.stringCoords.last() - layout.stringCoords.first() + 80f,
        )
    }
    drawRect(color = Color(0xFF3A2415), topLeft = neckTopLeft, size = neckSize)

    val stringPad = 40f
    for (f in 1..layout.geometry.fretCount) {
        val u = layout.geometry.fretX(f)
        val a = layout.toScreen(u, layout.stringCoords.first() - stringPad)
        val b = layout.toScreen(u, layout.stringCoords.last() + stringPad)
        drawLine(fretColor, a, b, strokeWidth = 3f)
    }
    run {
        val a = layout.toScreen(layout.nutU, layout.stringCoords.first() - stringPad)
        val b = layout.toScreen(layout.nutU, layout.stringCoords.last() + stringPad)
        drawLine(Color(0xFFE8E4DA), a, b, strokeWidth = 8f)
    }

    val midV = (layout.stringCoords.first() + layout.stringCoords.last()) / 2f
    for (f in markerFrets) {
        if (f > layout.geometry.fretCount) continue
        val c = layout.toScreen(layout.geometry.fretCenterX(f), midV)
        drawCircle(markerColor, radius = 9f, center = c)
    }
    for (f in doubleMarkerFrets) {
        if (f > layout.geometry.fretCount) continue
        val u = layout.geometry.fretCenterX(f)
        val span = layout.stringCoords.last() - layout.stringCoords.first()
        for (frac in listOf(0.25f, 0.75f)) {
            val c = layout.toScreen(u, layout.stringCoords.first() + span * frac)
            drawCircle(markerColor, radius = 9f, center = c)
        }
    }

    val count = layout.stringCoords.size
    for (i in layout.stringCoords.indices) {
        val v = layout.stringCoords[i]
        val thickness = if (count > 1) 5f - 3.5f * i / (count - 1) else 3f
        val a = layout.toScreen(-layout.nutU * 0.8f, v)
        val b = layout.toScreen(layout.bridgeU, v)
        drawLine(Color(0xFFD8D8DC), a, b, strokeWidth = thickness, alpha = 0.9f)
    }

    for ((_, note) in pressed) {
        val pos = note.position
        val u = layout.geometry.fretCenterX(pos.fret)
        val v = layout.stringCoords[pos.stringIndex]
        val center = layout.toScreen(u, v)
        val t = (nowNanos - note.startNanos) / 1e9f
        val amp = 6f * exp(-t / 0.6).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0f)),
                center = center,
                radius = 34f,
            ),
            radius = 34f,
            center = center,
        )
        drawCircle(accent, radius = 8f, center = center)

        if (amp > 0.2f) {
            val path = Path()
            val from = u - 50f
            val to = u + 50f
            var first = true
            var uu = from
            while (uu <= to) {
                val off = amp * sin(2 * Math.PI * 11 * t) * sin(Math.PI * (uu - from) / (to - from))
                val p = layout.toScreen(uu, v + off)
                if (first) {
                    path.moveTo(p.x, p.y)
                    first = false
                } else {
                    path.lineTo(p.x, p.y)
                }
                uu += 4f
            }
            drawPath(path, accent.copy(alpha = 0.8f), style = Stroke(width = 2f))
        }

        if (settings.showNoteNames) {
            drawContext.canvas.nativeCanvas.drawText(
                Notes.nameOf(pos.midi),
                center.x,
                if (layout.vertical) center.y - 30f else center.y - 34f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                },
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onTuningSelected: (String) -> Unit,
    onFretCountChanged: (Int) -> Unit,
    onVerticalChanged: (Boolean) -> Unit,
    onLeftHandedChanged: (Boolean) -> Unit,
    onNoteNamesChanged: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("Tuning", style = MaterialTheme.typography.titleSmall)
                Tuning.All.forEach { tuning ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = tuning.id == settings.tuningId,
                            onClick = { onTuningSelected(tuning.id) },
                        )
                        Text(tuning.label)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Frets: ${settings.fretCount}", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = settings.fretCount.toFloat(),
                    onValueChange = { onFretCountChanged(it.toInt()) },
                    valueRange = FretboardGeometry.MIN_FRET_COUNT.toFloat()..FretboardGeometry.MAX_FRET_COUNT.toFloat(),
                    steps = FretboardGeometry.MAX_FRET_COUNT - FretboardGeometry.MIN_FRET_COUNT - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingSwitch("Vertical neck", settings.vertical, onVerticalChanged)
                SettingSwitch("Left-handed", settings.leftHanded, onLeftHandedChanged)
                SettingSwitch("Note names on tap", settings.showNoteNames, onNoteNamesChanged)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}
