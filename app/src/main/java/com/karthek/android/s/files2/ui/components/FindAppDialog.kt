package com.karthek.android.s.files2.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthek.android.s.files2.R


@Composable
fun FindAppDialog(onDismissCallback: () -> Unit, onFindCallback: () -> Unit) {
    Dialog(onDismissRequest = onDismissCallback, confirmButton = {
        TextButton(onClick = onFindCallback) {
            Text(text = stringResource(id = android.R.string.search_go))
        }
    }, dismissButton = {
        TextButton(onClick = onDismissCallback) {
            Text(text = stringResource(id = android.R.string.cancel))
        }
    }) {
        Text(
            text = stringResource(id = R.string.play),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        )
    }
}

fun Context.findOnGooglePlay(mimeType: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse("http://play.google.com/store/search?q=$mimeType&c=apps")
    intent.setPackage("com.android.vending")
    startActivity(intent)
}