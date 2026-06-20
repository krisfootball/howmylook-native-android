package com.howmylook.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

private const val GRID_DECODE_PX = 480
private const val AVATAR_DECODE_PX = 156

@Composable
fun GridPostImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    if (imageUrl.isNullOrBlank()) return

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(150)
            .size(Size(GRID_DECODE_PX, GRID_DECODE_PX))
            .memoryCacheKey(imageUrl)
            .diskCacheKey(imageUrl)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
fun AvatarPostImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    if (imageUrl.isNullOrBlank()) return

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(150)
            .size(Size(AVATAR_DECODE_PX, AVATAR_DECODE_PX))
            .memoryCacheKey(imageUrl)
            .diskCacheKey(imageUrl)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
