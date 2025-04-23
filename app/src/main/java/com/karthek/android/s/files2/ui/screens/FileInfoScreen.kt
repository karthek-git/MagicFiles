package com.karthek.android.s.files2.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.karthek.android.s.files2.FileInfoActivity

@Composable
fun FileInfoContent(fMedia: FileInfoActivity.FMedia) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

        if (fMedia.mimeType.startsWith("image/")) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(fMedia.uri).build(),
                contentDescription = "",
                modifier = Modifier
                    .height(240.dp)
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "Android",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}