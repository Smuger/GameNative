package app.gamenative.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val detectedLayout by logger.detectedLayout
    val inputPaths = remember(isRelativeMouse, winHandlerConnected) {
        logger.describeInputPaths(isRelativeMouse, winHandlerConnected)
    }
    val wineDebugActive = wineDebugChannels != "-all" && wineDebugChannels.isNotEmpty()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = PluviaTheme.colors.accentCyan,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Input Debug",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = PluviaTheme.colors.accentCyan,
                        )
                    }
                    Row {
                        TextButton(onClick = { logger.clear() }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PluviaTheme.colors.accentWarning,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = PluviaTheme.colors.accentWarning, fontSize = 12.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatBadge("Keys", keyCount.toString(), PluviaTheme.colors.accentCyan)
                    StatBadge("Mouse", mouseCount.toString(), PluviaTheme.colors.accentPurple)
                    StatBadge("Unmapped", unmappedCount.toString(), PluviaTheme.colors.accentDanger)
                    StatBadge("Wine", wineCount.toString(), PluviaTheme.colors.accentWarning)
                    StatBadge(
                        "Ptr",
                        "$pointerX,$pointerY",
                        PluviaTheme.colors.accentSuccess,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Current mouse/screen info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoChip(
                        label = "Mouse",
                        value = if (isRelativeMouse) "RELATIVE" else "ABSOLUTE",
                        color = if (isRelativeMouse) PluviaTheme.colors.accentWarning else PluviaTheme.colors.accentSuccess,
                    )
                    InfoChip(
                        label = "Screen",
                        value = "${screenWidth}x${screenHeight}",
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    InfoChip(
                        label = "WinHandler",
                        value = if (winHandlerConnected) "OK" else "WAIT",
                        color = if (winHandlerConnected) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Wine debug status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoChip(
                        label = "WINEDEBUG",
                        value = if (wineDebugActive) wineDebugChannels.take(40) else "DISABLED",
                        color = if (wineDebugActive) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger,
                    )
                    if (detectedLayout != null) {
                        InfoChip(
                            label = "Layout",
                            value = detectedLayout!!,
                            color = PluviaTheme.colors.accentCyan,
                        )
                    }
                }

                if (!wineDebugActive) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wine-side logging disabled. Enable in Settings > Debug > Wine Logs with channels: keyboard,key,cursor,event. Requires game restart.",
                        color = PluviaTheme.colors.accentDanger.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                // Input paths
                Text(
                    text = "INPUT PATHS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(6.dp))
                inputPaths.forEach { path ->
                    InputPathRow(path)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                // Event log header
                Text(
                    text = "EVENT LOG (${events.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(6.dp))

                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Press keys or move mouse to see events...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun InputPathRow(path: InputDebugLogger.InputPathInfo) {
    val statusColor = if (path.active) PluviaTheme.colors.accentSuccess else Color.White.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = path.name,
                    color = if (path.active) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "[${path.protocol}]",
                    color = PluviaTheme.colors.accentPurple.copy(alpha = if (path.active) 0.8f else 0.3f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = path.path,
                color = Color.White.copy(alpha = if (path.active) 0.4f else 0.15f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
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
        InputDebugLogger.EventType.WINE_KEY -> Color(0xFFFF6B35)
        InputDebugLogger.EventType.WINE_MOUSE -> Color(0xFFFF35A0)
        InputDebugLogger.EventType.WINE_LAYOUT -> Color(0xFF35BFFF)
        InputDebugLogger.EventType.WINE_INFO -> Color(0xFF8888AA)
    }

    val mappedColor = if (event.mapped) PluviaTheme.colors.accentSuccess else PluviaTheme.colors.accentDanger

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (!event.mapped) PluviaTheme.colors.accentDanger.copy(alpha = 0.05f)
                else Color.Transparent,
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Timestamp
        Text(
            text = timeFormat.format(Date(event.timestamp)),
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )

        // Type badge
        Text(
            text = when (event.type) {
                InputDebugLogger.EventType.KEY_DOWN -> "KEY↓"
                InputDebugLogger.EventType.KEY_UP -> "KEY↑"
                InputDebugLogger.EventType.MOUSE_MOVE -> "MOV"
                InputDebugLogger.EventType.MOUSE_BUTTON -> "BTN"
                InputDebugLogger.EventType.TOUCH -> "TCH"
                InputDebugLogger.EventType.GAMEPAD -> "PAD"
                InputDebugLogger.EventType.WINE_KEY -> "W:K"
                InputDebugLogger.EventType.WINE_MOUSE -> "W:M"
                InputDebugLogger.EventType.WINE_LAYOUT -> "W:L"
                InputDebugLogger.EventType.WINE_INFO -> "W:I"
            },
            color = typeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(typeColor.copy(alpha = 0.1f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )

        // Source
        Text(
            text = event.source,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )

        // Mapped indicator
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(mappedColor),
        )

        // Details
        Text(
            text = event.details,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        )
    }
}
