package com.rockmusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rockmusic.app.presentation.RockMusicRoot
import com.rockmusic.app.presentation.integrations.IntegrationConnectionsScreen
import com.rockmusic.app.presentation.theme.RockMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RockMusicTheme {
                var showConnections by rememberSaveable { mutableStateOf(false) }

                Box(Modifier.fillMaxSize()) {
                    if (showConnections) {
                        IntegrationConnectionsScreen(
                            onClose = { showConnections = false },
                        )
                    } else {
                        RockMusicRoot()
                        SmallFloatingActionButton(
                            onClick = { showConnections = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 48.dp, end = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Open provider connections",
                            )
                        }
                    }
                }
            }
        }
    }
}
