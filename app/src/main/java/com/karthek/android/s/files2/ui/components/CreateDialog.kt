package com.karthek.android.s.files2.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.runtime.Composable

@Composable
fun EditTextAlertDialog(onDismissRequest: () -> Unit, title: String) {
    AlertDialog(onDismissRequest = onDismissRequest, title = {}, confirmButton = {})
}