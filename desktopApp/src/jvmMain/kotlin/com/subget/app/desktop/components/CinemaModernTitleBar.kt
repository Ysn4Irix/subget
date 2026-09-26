package com.subget.app.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.subget.app.ui.components.BrandLogo

@Composable
fun WindowScope.CinemaModernTitleBar(
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF0B0E14))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Draggable Area for Window Title & Icon
            WindowDraggableArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandLogo(
                        isDark = true,
                        height = 20.dp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "SUBGET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFF1F5F9)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(Color(0xFF475569), RoundedCornerShape(1.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Cinema Subtitle Downloader",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Window Controls (Non-draggable to ensure instant click response)
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Button
                TitleBarButton(
                    onClick = { windowState.isMinimized = true },
                    hoverColor = Color.White.copy(alpha = 0.08f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.5.dp)
                            .background(Color(0xFF94A3B8))
                    )
                }

                // Maximize / Restore Button
                TitleBarButton(
                    onClick = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    hoverColor = Color.White.copy(alpha = 0.08f)
                ) {
                    if (isMaximized) {
                        // Restore Icon (overlapping squares)
                        Box(modifier = Modifier.size(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.BottomStart)
                                    .border(1.dp, Color(0xFF94A3B8))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .border(1.dp, Color(0xFF94A3B8))
                            )
                        }
                    } else {
                        // Maximize Icon (single square)
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .border(1.2.dp, Color(0xFF94A3B8))
                        )
                    }
                }

                // Close Button
                TitleBarButton(
                    onClick = onCloseRequest,
                    hoverColor = Color(0xFFE50914),
                    isClose = true
                ) { isHovered ->
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(15.dp),
                        tint = if (isHovered) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Bottom border line separating title bar from app content
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 0.5.dp,
            color = Color(0xFF1C2333)
        )
    }
}

@Composable
private fun TitleBarButton(
    onClick: () -> Unit,
    hoverColor: Color,
    isClose: Boolean = false,
    content: @Composable (isHovered: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (isHovered) hoverColor else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(isHovered)
    }
}
