package com.karthek.android.s.files2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthek.android.s.files2.helpers.ESMPermissionState
import com.karthek.android.s.files2.helpers.PermissionRequired
import com.karthek.android.s.files2.helpers.rememberESMPermissionState

@Composable
fun Perms(navigateToSettingsScreen: () -> Unit, content: @Composable () -> Unit) {
    val permissionState = rememberESMPermissionState()

    PermissionRequired(
        permissionState = permissionState,
        permissionNotGrantedContent = { PermissionNotGrantedContent(permissionState) },
        permissionNotAvailableContent = { PermissionNotAvailableContent(navigateToSettingsScreen) }
    ) {
        content()
    }
}

@Composable
fun PermissionNotGrantedContent(permissionState: ESMPermissionState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please grant the storage permission.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { permissionState.launchPermissionRequest() }) {
            Text(stringResource(android.R.string.ok))
        }
    }
}

@Composable
fun PermissionNotAvailableContent(navigateToSettingsScreen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Storage permission denied. Please, grant access on the Settings screen.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = navigateToSettingsScreen) {
            Text("Open Settings")
        }
    }
}