package com.example.antigravityremote

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antigravityremote.model.ConnectionState
import com.example.antigravityremote.ui.screens.ChatScreen
import com.example.antigravityremote.ui.screens.ConnectionScreen
import com.example.antigravityremote.ui.screens.DeviceDiscoveryScreen
import com.example.antigravityremote.ui.screens.GoogleAuthScreen
import com.example.antigravityremote.ui.viewmodel.RemoteViewModel

@Composable
fun MainNavigation(
    viewModel: RemoteViewModel = viewModel()
) {
    val activeWebUrl by viewModel.activeWebUrl.collectAsState()
    var isScanningQr by remember { mutableStateOf(false) }

    Crossfade(
        targetState = when {
            activeWebUrl != null -> "REMOTE"
            isScanningQr -> "SCANNER"
            else -> "CONNECT"
        },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            "REMOTE" -> {
                activeWebUrl?.let { url ->
                    com.example.antigravityremote.ui.screens.AntigravityWebScreen(
                        url = url,
                        onBack = { viewModel.unpairAndScanNew() }
                    )
                }
            }
            "SCANNER" -> {
                com.example.antigravityremote.ui.screens.QrScannerScreen(
                    onQrCodeScanned = { qrData ->
                        isScanningQr = false
                        viewModel.handleScannedQrCode(qrData)
                    },
                    onClose = { isScanningQr = false }
                )
            }
            "CONNECT" -> {
                com.example.antigravityremote.ui.screens.GoogleAuthScreen(
                    viewModel = viewModel,
                    onScanQrClick = { isScanningQr = true }
                )
            }
        }
    }
}
