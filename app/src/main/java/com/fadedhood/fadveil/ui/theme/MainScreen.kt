package com.fadedhood.fadveil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onStartOverlay: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pixel Off",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        ElevatedButton(
            onClick = { onStartOverlay() },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Start Overlay")
        }

        ElevatedButton(
            onClick = { onRequestPermission() }
        ) {
            Text("Request Permission")
        }
    }
}