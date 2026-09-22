package com.subget.app.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import com.subget.app.R
import com.subget.app.data.api.models.SearchSuggestion
import coil.compose.AsyncImage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subget.app.ui.components.DownloadStatus
import com.subget.app.ui.components.LanguageFilterChips
import com.subget.app.ui.components.MediaHeaderCard
import com.subget.app.ui.components.MediaHeaderCardSkeleton
import com.subget.app.ui.components.SearchResultsSkeleton
import com.subget.app.ui.components.SubtitleCard
import com.subget.app.ui.components.SubtitleDetailsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var qualityMenuExpanded by remember { mutableStateOf(false) }
    var seriesMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isDark = com.subget.app.ui.theme.LocalIsDarkTheme.current
                        Image(
                            painter = painterResource(id = if (isDark) R.drawable.brand_logo_white else R.drawable.brand_logo),
                            contentDescription = "Subget Brand Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "SUBGET",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (uiState.hasApiKey) "SubDL • Downloader" else "API Key Needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.hasApiKey) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Missing API Key Warning Banner
            AnimatedVisibility(
                visible = !uiState.hasApiKey,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToSettings() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SubDL API Key Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap here to configure your free key in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Search Bar Input Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { focusRequester.requestFocus() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSearchFocused) 1.5.dp else 1.dp,
                    color = if (isSearchFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "Search movie or TV series...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.searchSubtitles()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                    }
                    if (uiState.query.isNotEmpty()) {
                        if (uiState.isLoading || uiState.isSuggestionsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.clearQuery()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.searchSubtitles()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            // Title Suggestions Dropdown
            BackHandler(enabled = uiState.showSuggestions) {
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.dismissSuggestions()
            }

            AnimatedVisibility(
                visible = uiState.showSuggestions && uiState.suggestions.isNotEmpty(),
                enter = fadeIn(tween(150)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                SearchSuggestionsDropdown(
                    suggestions = uiState.suggestions,
                    onSuggestionSelected = { suggestion ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.onSuggestionSelected(suggestion)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Language Filter Chips
            LanguageFilterChips(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = { code ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onLanguageSelected(code)
                }
            )

            // Main Body Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = uiState.showSuggestions
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.dismissSuggestions()
                    }
            ) {
                when {
                    uiState.isLoading && uiState.subtitles.isEmpty() -> {
                        SearchResultsSkeleton(isPosterLoading = uiState.isPosterLoading)
                    }

                    uiState.errorMessage != null && uiState.subtitles.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.mediaPoster != null) {
                                    MediaHeaderCard(
                                        poster = uiState.mediaPoster!!,
                                        totalReleases = 0,
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )
                                } else if (uiState.isPosterLoading) {
                                    MediaHeaderCardSkeleton(modifier = Modifier.padding(bottom = 20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!uiState.hasApiKey) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToSettings,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Configure API Key", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    uiState.subtitles.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Recent Searches Row (if any)
                            if (uiState.recentSearches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RECENT SEARCHES",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.clearRecentSearches() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Clear",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.recentSearches.forEach { recent ->
                                        Surface(
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.onRecentSearchClicked(recent)
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = recent,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.removeRecentSearch(recent) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Empty State Art
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Subtitles,
                                            contentDescription = null,
                                            modifier = Modifier.size(38.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Discover Cinema Subtitles",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Search by movie title, series name, or IMDb ID to browse quality-grouped releases.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        val grouped = uiState.filteredAndSortedGroups

                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = if (uiState.isLoading) 0.65f else 1.0f)
                        ) {
                            // Filter & Sort Control Bar (Zero-Scroll Adaptive Layout)
                            item(key = "filter_sort_bar") {
                                val hasSeasons = uiState.availableSeasons.isNotEmpty()
                                val sortWeight = if (hasSeasons) 1.15f else 1.25f
                                val qualityWeight = if (hasSeasons) 1.0f else 1.1f
                                val seasonWeight = 1.0f
                                val hiWeight = if (hasSeasons) 0.85f else 0.85f

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Sort Selector Button
                                    Box(modifier = Modifier.weight(sortWeight)) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { sortMenuExpanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = "Sort",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = uiState.sortOption.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = sortMenuExpanded,
                                            onDismissRequest = { sortMenuExpanded = false },
                                            shape = RoundedCornerShape(14.dp),
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 8.dp,
                                            modifier = Modifier
                                                .widthIn(min = 180.dp)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            SubtitleSortOption.entries.forEach { option ->
                                                val isSelected = option == uiState.sortOption
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = option.displayName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (isSelected) {
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
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier
                                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                            } else {
                                                                Modifier
                                                            }
                                                        ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    onClick = {
                                                        viewModel.setSortOption(option)
                                                        sortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Quality Selector Button
                                    Box(modifier = Modifier.weight(qualityWeight)) {
                                        val isQualityActive = uiState.selectedQuality != QualityFilterOption.ALL
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { qualityMenuExpanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isQualityActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isQualityActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HighQuality,
                                                    contentDescription = "Quality Filter",
                                                    tint = if (isQualityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = uiState.selectedQuality.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isQualityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isQualityActive) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = if (isQualityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = qualityMenuExpanded,
                                            onDismissRequest = { qualityMenuExpanded = false },
                                            shape = RoundedCornerShape(14.dp),
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 8.dp,
                                            modifier = Modifier
                                                .widthIn(min = 210.dp)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            val counts = uiState.qualityCounts
                                            QualityFilterOption.entries.forEach { option ->
                                                val count = counts[option] ?: 0
                                                val isSelected = option == uiState.selectedQuality
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = option.dropdownLabel,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                            ) {
                                                                Text(
                                                                    text = "$count",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier
                                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                            } else {
                                                                Modifier
                                                            }
                                                        ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    onClick = {
                                                        viewModel.setQualityFilter(option)
                                                        qualityMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 3. Unified TV Series Season & Episode Button (Contextual for series)
                                    if (hasSeasons) {
                                        val isSeriesActive = uiState.selectedSeason != null || uiState.selectedEpisode != null
                                        val seriesLabel = when {
                                            uiState.selectedSeason != null && uiState.selectedEpisode != null ->
                                                "S${uiState.selectedSeason}:E%02d".format(uiState.selectedEpisode)
                                            uiState.selectedSeason != null ->
                                                "S${uiState.selectedSeason}"
                                            uiState.selectedEpisode != null ->
                                                "Ep ${uiState.selectedEpisode}"
                                            else -> "Season"
                                        }

                                        Box(modifier = Modifier.weight(seasonWeight)) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { seriesMenuExpanded = true },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSeriesActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSeriesActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tv,
                                                        contentDescription = "Series Filter",
                                                        tint = if (isSeriesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = seriesLabel,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSeriesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = if (isSeriesActive) FontWeight.Bold else FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        tint = if (isSeriesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = seriesMenuExpanded,
                                                onDismissRequest = { seriesMenuExpanded = false },
                                                shape = RoundedCornerShape(14.dp),
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                shadowElevation = 8.dp,
                                                modifier = Modifier
                                                    .widthIn(min = 230.dp, max = 310.dp)
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                val seasonCounts = uiState.seasonCounts
                                                val isAllSeasonSelected = uiState.selectedSeason == null

                                                Text(
                                                    text = "SEASONS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )

                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "All Seasons",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isAllSeasonSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isAllSeasonSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = if (isAllSeasonSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                            ) {
                                                                Text(
                                                                    text = "${seasonCounts[null] ?: 0}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (isAllSeasonSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .then(
                                                            if (isAllSeasonSelected) {
                                                                Modifier
                                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                            } else {
                                                                Modifier
                                                            }
                                                        ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setSeasonFilter(null)
                                                        seriesMenuExpanded = false
                                                    }
                                                )

                                                uiState.availableSeasons.forEach { seasonNum ->
                                                    val count = seasonCounts[seasonNum] ?: 0
                                                    val isSelected = uiState.selectedSeason == seasonNum
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "Season $seasonNum",
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                                ) {
                                                                    Text(
                                                                        text = "$count",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .then(
                                                                if (isSelected) {
                                                                    Modifier
                                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                                } else {
                                                                    Modifier
                                                                }
                                                            ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.setSeasonFilter(seasonNum)
                                                            seriesMenuExpanded = false
                                                        }
                                                    )
                                                }

                                                // Episodes Section
                                                if (uiState.availableEpisodes.isNotEmpty()) {
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    val episodeCounts = uiState.episodeCounts
                                                    val isAllEpSelected = uiState.selectedEpisode == null

                                                    Text(
                                                        text = if (uiState.selectedSeason != null) "SEASON ${uiState.selectedSeason} EPISODES" else "EPISODES",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                    )

                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "All Episodes",
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = if (isAllEpSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    color = if (isAllEpSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (isAllEpSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                                ) {
                                                                    Text(
                                                                        text = "${episodeCounts[null] ?: 0}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = if (isAllEpSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .then(
                                                                if (isAllEpSelected) {
                                                                    Modifier
                                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                                } else {
                                                                    Modifier
                                                                }
                                                            ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.setEpisodeFilter(null)
                                                            seriesMenuExpanded = false
                                                        }
                                                    )

                                                    uiState.availableEpisodes.forEach { epNum ->
                                                        val count = episodeCounts[epNum] ?: 0
                                                        val isSelected = uiState.selectedEpisode == epNum
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "Episode $epNum",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Spacer(modifier = Modifier.width(16.dp))
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                                    ) {
                                                                        Text(
                                                                            text = "$count",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                            fontWeight = FontWeight.Bold,
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .then(
                                                                    if (isSelected) {
                                                                        Modifier
                                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                ),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                            onClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.setEpisodeFilter(epNum)
                                                                seriesMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 4. HI Only Filter Toggle Pill
                                    Surface(
                                        modifier = Modifier
                                            .weight(hiWeight)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleHiOnly()
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (uiState.hiOnly) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (uiState.hiOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Hearing,
                                                contentDescription = null,
                                                tint = if (uiState.hiOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "HI",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (uiState.hiOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (uiState.hiOnly) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Media Poster Artwork Preview
                            if (uiState.mediaPoster != null) {
                                item(key = "media_poster_header") {
                                    ScrollAnimatedItem(delayMillis = 0) {
                                        MediaHeaderCard(
                                            poster = uiState.mediaPoster!!,
                                            totalReleases = uiState.subtitles.size
                                        )
                                    }
                                }
                            } else if (uiState.isPosterLoading) {
                                item(key = "media_poster_skeleton") {
                                    MediaHeaderCardSkeleton()
                                }
                            }

                            // Grouped Releases
                            grouped.forEach { (tierName, itemsInTier) ->
                                item(key = "header_$tierName") {
                                    ScrollAnimatedItem(delayMillis = 0) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, bottom = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = tierName.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Text(
                                                text = "${itemsInTier.size} release${if (itemsInTier.size != 1) "s" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                itemsIndexed(
                                    items = itemsInTier,
                                    key = { _, item -> item.fullDownloadUrl.ifBlank { item.displayTitle + item.displayLanguage + item.comment.orEmpty() } }
                                ) { index, item ->
                                    val status = uiState.downloadStatuses[item.fullDownloadUrl] ?: DownloadStatus.Idle
                                    ScrollAnimatedItem(
                                        delayMillis = (index.coerceAtMost(5) * 35),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        SubtitleCard(
                                            item = item,
                                            status = status,
                                            onDownloadClick = { viewModel.downloadSubtitle(item) },
                                            onShareClick = { viewModel.shareSubtitle(item, context) },
                                            onCardClick = { viewModel.selectSubtitleForDetails(item) }
                                        )
                                    }
                                }
                            }

                            if (grouped.isEmpty()) {
                                item(key = "empty_filter_results") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val activeFilters = buildList {
                                            if (uiState.selectedQuality != QualityFilterOption.ALL) {
                                                add(uiState.selectedQuality.dropdownLabel)
                                            }
                                            if (uiState.selectedSeason != null) {
                                                add("Season ${uiState.selectedSeason}")
                                            }
                                            if (uiState.selectedEpisode != null) {
                                                add("Episode ${uiState.selectedEpisode}")
                                            }
                                            if (uiState.hiOnly) {
                                                add("HI Only")
                                            }
                                        }
                                        val msg = if (activeFilters.isNotEmpty()) {
                                            "No releases found matching " + activeFilters.joinToString(", ")
                                        } else {
                                            "No releases found"
                                        }
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.resetAllFilters()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Reset All Filters")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Details Bottom Sheet
        if (uiState.selectedSubtitleForDetails != null) {
            val selected = uiState.selectedSubtitleForDetails!!
            val status = uiState.downloadStatuses[selected.fullDownloadUrl] ?: DownloadStatus.Idle
            SubtitleDetailsBottomSheet(
                item = selected,
                status = status,
                sheetState = sheetState,
                onDismiss = { viewModel.selectSubtitleForDetails(null) },
                onDownloadClick = { viewModel.downloadSubtitle(selected) },
                onShareClick = { viewModel.shareSubtitle(selected, context) }
            )
        }
    }
}

@Composable
private fun ScrollAnimatedItem(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "scroll_alpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "scroll_translate_y"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.96f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "scroll_scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translateY
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

@Composable
private fun SearchSuggestionsDropdown(
    suggestions: List<SearchSuggestion>,
    onSuggestionSelected: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val itemsToShow = suggestions.take(6)
            itemsToShow.forEachIndexed { index, suggestion ->
                SearchSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionSelected(suggestion) }
                )
                if (index < itemsToShow.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster thumbnail
        Box(
            modifier = Modifier
                .size(34.dp, 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!suggestion.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = suggestion.posterUrl,
                    contentDescription = suggestion.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (suggestion.mediaType.equals("TV Series", ignoreCase = true) ||
                        suggestion.mediaType.equals("Series", ignoreCase = true)
                    ) {
                        Icons.Default.Tv
                    } else {
                        Icons.Default.Movie
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!suggestion.mediaType.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = suggestion.mediaType,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                if (!suggestion.year.isNullOrBlank()) {
                    Text(
                        text = suggestion.year,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Trailing arrow indicator
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Select",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
