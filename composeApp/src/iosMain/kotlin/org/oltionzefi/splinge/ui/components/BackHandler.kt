package org.oltionzefi.splinge.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button
}
