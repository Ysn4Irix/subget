package com.subget.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LanguageOption(val code: String?, val label: String, val flag: String)

val PINNED_LANGUAGES = listOf(
    LanguageOption(null, "All", "🌐"),
    LanguageOption("en", "English", "🇺🇸"),
    LanguageOption("ar", "Arabic", "🇸🇦")
)

val OTHER_LANGUAGES = listOf(
    LanguageOption("es", "Spanish", "🇪🇸"),
    LanguageOption("fr", "French", "🇫🇷"),
    LanguageOption("de", "German", "🇩🇪"),
    LanguageOption("pt", "Portuguese", "🇧🇷"),
    LanguageOption("it", "Italian", "🇮🇹"),
    LanguageOption("fa", "Persian", "🇮🇷"),
    LanguageOption("tr", "Turkish", "🇹🇷")
)

val ALL_LANGUAGES = PINNED_LANGUAGES + OTHER_LANGUAGES

@Composable
fun LanguageFilterChips(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val selectedOtherLanguage = OTHER_LANGUAGES.firstOrNull { it.code == selectedLanguage }
    val isOtherSelected = selectedOtherLanguage != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PINNED_LANGUAGES.forEach { option ->
            val isSelected = selectedLanguage == option.code

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "chipBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "chipText"
            )
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(19.dp))
                    .clickable { onLanguageSelected(option.code) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = option.flag,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
        ) {
            val backgroundColor by animateColorAsState(
                targetValue = if (isOtherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "moreChipBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isOtherSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "moreChipText"
            )
            val borderColor = if (isOtherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

            val displayFlag = selectedOtherLanguage?.flag ?: "🌍"
            val displayLabel = selectedOtherLanguage?.label ?: "More"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(19.dp))
                    .clickable { moreMenuExpanded = true }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayFlag,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = if (isOtherSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "More languages",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .padding(vertical = 4.dp)
            ) {
                OTHER_LANGUAGES.forEach { option ->
                    val isCurrent = selectedLanguage == option.code
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = option.flag, fontSize = 16.sp)
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isCurrent) {
                                    Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                }
                            ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            onLanguageSelected(option.code)
                            moreMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
