package com.subget.app.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop window back handling (no-op as window has standard navigation)
}
