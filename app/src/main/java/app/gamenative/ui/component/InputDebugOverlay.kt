package app.gamenative.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gamenative.ui.theme.PluviaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Non-blocking debug HUD. Sits at the bottom of the screen and does NOT
 * consume touch events in the game area above it. The game keeps running
 * and receiving input while this is visible.
 */
@Composable
fun InputDebugOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isRelativeMouse: Boolean,
    winHandlerConnected: Boolean,
    pointerX: Int,
    pointerY: Int,
    screenWidth: Int,
    screenHeight: Int,
    wineDebugChannels: String,
    modifier: Modifier = Modifier,
) {
    val logger = InputDebugLogger
    val events = logger.events
    val keyCount by logger.keyboardEventCount
    val mouseCount by logger.mouseEventCount
    val unmappedCount by logger.unmappedKeyCount
    val wineCount by logger.wineEventCount
    val x11Count by logger.x11KeyCount
    val detectedLayout by logger.detectedLayout
    val wineDebugActive = wineDebugChannels != "-all" && wineDebugChannels.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The panel anchored to the bottom — only this part blocks touches
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(
                        if (expanded) Modifier.fillMaxHeight(0.55f)
                        else Modifier
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = PluviaTheme.colors.accentCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Input Debug",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PluviaTheme.colors.accentCyan,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { logger.clear() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, null, Modifier.size(14.dp), tint = PluviaTheme.colors.accentWarning)
                        }
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                null, Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f),
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Stats row — always visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatBadge("Keys", keyCount.toString(), PluviaTheme.colors.accentCyan)
                    StatBadge("X11", x11Count.toString(), Color(0xFF00E5FF))
                    StatBadge("Wine", wineCount.toString(), PluviaTheme.colors.accentWarning)
                    StatBadge("Bad", unmappedCount.toString(), PluviaTheme.colors.accentDanger)
                    StatBadge("Ptr", "$pointerX,$pointerY", PluviaTheme.colors.accentSuccess)
                }

                Spacer(Modifier.height(4.dp))

                // Info chips row — always visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfoChip(
                        "Mouse",
                        if (isRelativeMouse) "REL" else "ABS",
                        if (isRelativeMouse) PluviaTheme.colors.accentWarning else PluviaTheme.colors.accentSuccess,
                    )
                    InfoChip(
                        "Scr",
                        "${screenWidth}x${screenHeight}",
                        Color.White.copy(alpha = 0.5f),
                    )
                    InfoChip(
                        "WH",
                        if (winHandlerConnected) "OK" else "WAIT",
                        if (winHandlerConnected) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger,
                    )
                    InfoChip(
                        "WineDbg",
                        if (wineDebugActive) "ON" else "OFF",
                        if (wineDebugActive) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger,
                    )
                    if (detectedLayout != null) {
                        InfoChip("Layout", detectedLayout!!, PluviaTheme.colors.accentCyan)
                    }
                }

                // Expanded content
                if (expanded) {
                    Spacer(Modifier.height(6.dp))

                    if (!wineDebugActive) {
                        Text(
                            "Wine logging OFF. Enable: Settings > Debug > Wine Logs (keyboard,key,cursor,event). Restart game.",
                            color = PluviaTheme.colors.accentDanger.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(4.dp))

                    // System trace log
                    val sysLog = logger.systemLog
                    if (sysLog.isNotEmpty()) {
                        Text(
                            "SYSTEM TRACE (${sysLog.size})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = PluviaTheme.colors.accentWarning.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(2.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            items(sysLog.size, key = { sysLog[it].hashCode() + it }) { idx ->
                                val entry = sysLog[idx]
                                val c = when {
                                    entry.contains("Wine:") -> Color(0xFFFF6B35)
                                    entry.contains("X11 Server:") -> PluviaTheme.colors.accentCyan
                                    entry.contains("Android:") -> PluviaTheme.colors.accentSuccess
                                    else -> Color.White.copy(alpha = 0.4f)
                                }
                                Text(
                                    entry, color = c.copy(alpha = 0.8f),
                                    fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(Modifier.height(4.dp))
                    }

                    // Event log
                    Text(
                        "EVENT LOG (${events.size})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(2.dp))
                    if (events.isEmpty()) {
                        Text(
                            "Play the game — events appear here live...",
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            items(events, key = { it.timestamp }) { event ->
                                EventRow(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 8.sp)
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp)
        Text(value, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun EventRow(event: InputDebugLogger.DebugEvent) {
    val typeColor = when (event.type) {
        InputDebugLogger.EventType.KEY_DOWN -> PluviaTheme.colors.accentCyan
        InputDebugLogger.EventType.KEY_UP -> PluviaTheme.colors.accentCyan.copy(alpha = 0.5f)
        InputDebugLogger.EventType.MOUSE_MOVE -> PluviaTheme.colors.accentPurple.copy(alpha = 0.6f)
        InputDebugLogger.EventType.MOUSE_BUTTON -> PluviaTheme.colors.accentPurple
        InputDebugLogger.EventType.TOUCH -> PluviaTheme.colors.accentPink
        InputDebugLogger.EventType.GAMEPAD -> PluviaTheme.colors.accentWarning
        InputDebugLogger.EventType.X11_KEY -> Color(0xFF00E5FF)
        InputDebugLogger.EventType.WINE_KEY -> Color(0xFFFF6B35)
        InputDebugLogger.EventType.WINE_MOUSE -> Color(0xFFFF35A0)
        InputDebugLogger.EventType.WINE_LAYOUT -> Color(0xFF35BFFF)
        InputDebugLogger.EventType.WINE_INFO -> Color(0xFF8888AA)
    }
    val mappedColor = if (event.mapped) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(if (!event.mapped) PluviaTheme.colors.accentDanger.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            timeFormat.format(Date(event.timestamp)),
            color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp, fontFamily = FontFamily.Monospace,
        )
        Text(
            when (event.type) {
                InputDebugLogger.EventType.KEY_DOWN -> "KEY↓"
                InputDebugLogger.EventType.KEY_UP -> "KEY↑"
                InputDebugLogger.EventType.MOUSE_MOVE -> "MOV"
                InputDebugLogger.EventType.MOUSE_BUTTON -> "BTN"
                InputDebugLogger.EventType.TOUCH -> "TCH"
                InputDebugLogger.EventType.GAMEPAD -> "PAD"
                InputDebugLogger.EventType.X11_KEY -> "X11"
                InputDebugLogger.EventType.WINE_KEY -> "W:K"
                InputDebugLogger.EventType.WINE_MOUSE -> "W:M"
                InputDebugLogger.EventType.WINE_LAYOUT -> "W:L"
                InputDebugLogger.EventType.WINE_INFO -> "W:I"
            },
            color = typeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(typeColor.copy(alpha = 0.1f))
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
        Text(event.source, color = Color.White.copy(alpha = 0.35f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(Modifier.size(4.dp).clip(CircleShape).background(mappedColor))
        Text(
            event.details, color = Color.White.copy(alpha = 0.65f),
            fontSize = 8.sp, fontFamily = FontFamily.Monospace,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        )
    }
}
