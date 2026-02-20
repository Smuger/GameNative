package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.ui.enums.LibraryTab
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.WindowWidthClass
import app.gamenative.ui.util.rememberWindowWidthClass

/**
 * Tab bar for library navigation with sliding pill indicator.
 * Adapts to screen width
 */
@Composable
fun LibraryTabBar(
    currentTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberWindowWidthClass()

    when (widthClass) {
        WindowWidthClass.COMPACT -> CompactLibraryTabBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            modifier = modifier,
        )

        else -> ExpandedLibraryTabBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            modifier = modifier,
        )
    }
}

/**
 * Compact tab bar for narrow screens.
 * Centered tabs with action buttons for Options, Search, Add Game, and Menu.
 */
@Composable
private fun CompactLibraryTabBar(
    currentTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = LibraryTab.entries

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(top = 8.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Left: Options button
            CompactIconButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            // Center: Tabs (takes remaining space, centered)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == currentTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                            )
                            .selectable(
                                selected = isSelected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) },
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(tab.labelResId),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            },
                        )
                    }
                }
            }

            // Right: Search, Add, Menu buttons
            CompactIconButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )
            CompactIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_game),
                onClick = onAddGameClick,
            )
            CompactIconButton(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                onClick = onMenuClick,
            )
        }
    }
}

/**
 * Simple icon button for compact tab bar.
 */
@Composable
private fun CompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .selectable(
                selected = false,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Expanded tab bar for wide screens (landscape phone, tablet).
 */
@Composable
private fun ExpandedLibraryTabBar(
    currentTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = LibraryTab.entries
    val currentIndex = tabs.indexOf(currentTab)

    // Track tab positions and widths for the sliding indicator
    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    val density = LocalDensity.current

    // Animated indicator position and width
    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (tabPositions[currentIndex] ?: 0f).toDp() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorOffset",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (tabWidths[currentIndex] ?: 80f).toDp() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorWidth",
    )

    // Gradient background container - content scrolls behind this
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(top = 8.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Options button (opens filter/sort panel)
            IconActionButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            // L1 button (previous tab)
            ShoulderButton(
                label = "L1",
                onClick = onPreviousTab,
                isLeft = true,
            )

            // Tab container with sliding indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            ),
                        ),
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Sliding pill indicator (rendered behind tabs)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(indicatorWidth)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                ),
                            ),
                        ),
                )

                // Tab items row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        TabItem(
                            tab = tab,
                            isSelected = tab == currentTab,
                            onClick = { onTabSelected(tab) },
                            onPositioned = { position, width ->
                                tabPositions[index] = position
                                tabWidths[index] = width
                            },
                        )
                    }
                }
            }

            // R1 button (next tab)
            ShoulderButton(
                label = "R1",
                onClick = onNextTab,
                isLeft = false,
            )

            // Search button
            IconActionButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )

            // Add game button
            IconActionButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_game),
                onClick = onAddGameClick,
            )

            // Menu button (opens system menu)
            IconActionButton(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                onClick = onMenuClick,
            )
        }
    }
}

@Composable
private fun ShoulderButton(
    label: String,
    onClick: () -> Unit,
    isLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "shoulderButtonScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "shoulderButtonAlpha",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isFocused) {
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        )
                    },
                ),
            )
            .then(
                if (isFocused) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isLeft) {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = label,
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconButtonScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconButtonAlpha",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isFocused) {
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        )
                    },
                ),
            )
            .then(
                if (isFocused) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TabItem(
    tab: LibraryTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val textAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isFocused -> 0.9f
            else -> 0.6f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textAlpha",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent().x,
                    coordinates.size.width.toFloat(),
                )
            }
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(tab.labelResId),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                currentTab = LibraryTab.ALL,
                onTabSelected = {},
                onPreviousTab = {},
                onNextTab = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar_Steam() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                currentTab = LibraryTab.STEAM,
                onTabSelected = {},
                onPreviousTab = {},
                onNextTab = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
            )
        }
    }
}
