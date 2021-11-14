package com.karthek.android.s.files2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.karthek.android.s.files2.state.FileListViewModel

@Composable
fun PrefsBottomSheet(viewModel: FileListViewModel) {

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(8.dp)
    ) {
        PrefsItemHeader(text = "GENERAL")
        PrefsItem(text = "Show hidden files") {
            Switch(
                checked = viewModel.showHidden,
                onCheckedChange = { viewModel.onShowHiddenChange(it) }
            )
        }
        PrefsItemHeader(text = "SORT BY")
        SortItem(text = "File name (A to Z)", 1, viewModel)
        SortItem(text = "File name (Z to A)", 2, viewModel)
        SortItem(text = "Size (smallest first)", 3, viewModel)
        SortItem(text = "Size (largest first)", 4, viewModel)
        SortItem(text = "Modified (oldest first)", 5, viewModel)
        SortItem(text = "Modified (newest first)", 6, viewModel)
    }
}

@Composable
fun PrefsItemHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun SortItem(text: String, index: Int, viewModel: FileListViewModel) {
    val selected = viewModel.sortPreference == index
    val color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
    PrefsItem(
        text = text,
        color = color,
        modifier = Modifier.clickable { viewModel.onSortPrefChanged(index) }) {
        if (selected) {
            Icon(imageVector = Icons.Outlined.Check, contentDescription = "", tint = color)
        }
    }
}

@Composable
fun PrefsItem(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface,
    content: @Composable () -> Unit
) {
    Row(modifier = modifier.padding(14.dp)) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}