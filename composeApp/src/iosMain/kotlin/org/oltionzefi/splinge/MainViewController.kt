package org.oltionzefi.splinge

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }.also {
    setRootViewController(it)
}