package com.karthek.android.s.files2.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class ESMPermissionState(private val context: Context, private val activity: Activity) {
    private var _hasPermission by mutableStateOf(checkPermission(context))
    var hasPermission: Boolean
        internal set(value) {
            _hasPermission = value
            refreshShouldShowRationale()
        }
        get() = _hasPermission

    var shouldShowRationale: Boolean by mutableStateOf(
        activity.shouldShowRationale()
    )
        private set

    var permissionRequested: Boolean by mutableStateOf(false)

    fun launchPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:com.karthek.android.s.files2")
                )
            )
        } else {
            launcher?.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) ?: throw IllegalStateException("ActivityResultLauncher cannot be null")
        }
    }

    internal var launcher: ActivityResultLauncher<String>? = null

    internal fun refreshHasPermission() {
        hasPermission = checkPermission(context)
    }

    private fun refreshShouldShowRationale() {
        shouldShowRationale = activity.shouldShowRationale()
    }
}

private fun checkPermission(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        context.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

@Composable
fun rememberESMPermissionState(): ESMPermissionState {
    val context = LocalContext.current
    val permissionState = remember { ESMPermissionState(context, context.findActivity()) }

    // Refresh the permission status when the lifecycle is resumed
    PermissionLifecycleCheckerEffect(permissionState)

    // Remember RequestPermission launcher and assign it to permissionState
    val launcher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionState.hasPermission = it
            permissionState.permissionRequested = true
        }
    }

    DisposableEffect(permissionState, launcher) {
        permissionState.launcher = launcher
        onDispose {
            permissionState.launcher = null
        }
    }

    return permissionState
}

@Composable
fun PermissionRequired(
    permissionState: ESMPermissionState,
    permissionNotGrantedContent: @Composable (() -> Unit),
    permissionNotAvailableContent: @Composable (() -> Unit),
    content: @Composable (() -> Unit),
) {
    when {
        permissionState.hasPermission -> {
            content()
        }
        permissionState.shouldShowRationale || !permissionState.permissionRequested -> {
            permissionNotGrantedContent()
        }
        else -> {
            permissionNotAvailableContent()
        }
    }
}

@Composable
private fun PermissionLifecycleCheckerEffect(
    permissionState: ESMPermissionState,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME
) {
    // Check if the permission was granted when the lifecycle is resumed.
    // The user might've gone to the Settings screen and granted the permission.
    val permissionCheckerObserver = remember(permissionState) {
        LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                // If the permission is revoked, check again.
                // We don't check if the permission was denied as that triggers a process restart.
                if (!permissionState.hasPermission) {
                    permissionState.refreshHasPermission()
                }
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, permissionCheckerObserver) {
        lifecycle.addObserver(permissionCheckerObserver)
        onDispose { lifecycle.removeObserver(permissionCheckerObserver) }
    }
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}

private fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

private fun Activity.shouldShowRationale(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        true
    } else {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
