package com.howmylook.app.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun ZoomableAsyncImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }

    fun resetZoom() {
        scale = MIN_ZOOM
        offset = Offset.Zero
    }

    fun clampOffset(rawOffset: Offset, containerWidth: Float, containerHeight: Float): Offset {
        if (scale <= MIN_ZOOM || containerWidth <= 0f || containerHeight <= 0f) {
            return Offset.Zero
        }
        val maxX = (containerWidth * (scale - MIN_ZOOM)) / 2f
        val maxY = (containerHeight * (scale - MIN_ZOOM)) / 2f
        return Offset(
            x = rawOffset.x.coerceIn(-maxX, maxX),
            y = rawOffset.y.coerceIn(-maxY, maxY),
        )
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(imageUrl) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > MIN_ZOOM + 0.01f) {
                                resetZoom()
                            } else {
                                scale = DOUBLE_TAP_ZOOM.coerceAtMost(MAX_ZOOM)
                            }
                        },
                        onTap = {
                            if (scale <= MIN_ZOOM + 0.01f) {
                                onClick?.invoke()
                            }
                        },
                    )
                }
                .pointerInput(imageUrl) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        if (newScale <= MIN_ZOOM + 0.01f) {
                            resetZoom()
                        } else {
                            scale = newScale
                            offset = clampOffset(offset + pan, containerWidth, containerHeight)
                        }
                    }
                },
        )
    }
}
