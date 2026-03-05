package org.oltionzefi.splinge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (dismissText != null) {
                        TextButton(onClick = onDismissRequest) {
                            Text(dismissText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = {
                        if (onConfirm != null) {
                            onConfirm()
                        } else {
                            onDismissRequest()
                        }
                    }) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaypalInputDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "PayPal Link", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Please enter your PayPal link (e.g., yourname) to open your 'Scan Me' page.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("PayPal Username") },
                    placeholder = { Text("yourname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Text("@", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                var sanitized = text.trim()
                                if (sanitized.startsWith("@")) {
                                    sanitized = sanitized.substring(1)
                                }
                                onSave(sanitized)
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Open")
                    }
                }
            }
        }
    }
}
