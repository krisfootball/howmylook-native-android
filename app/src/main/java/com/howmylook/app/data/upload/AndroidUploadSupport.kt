package com.howmylook.app.data.upload

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.UUID

private val imageMimeToExtension = mapOf(
    "image/jpeg" to "jpg",
    "image/jpg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
    "image/heic" to "heic",
    "image/heif" to "heif",
)

data class UploadPhotoPayload(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
)

fun loadUploadPhotoPayload(contentResolver: ContentResolver, uriString: String): UploadPhotoPayload {
    val uri = Uri.parse(uriString)
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val extension = imageMimeToExtension[mimeType] ?: "jpg"

    val bytes = contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        input.copyTo(output)
        output.toByteArray()
    } ?: error("Unable to read selected image.")

    val fileName = "${UUID.randomUUID()}.$extension"

    return UploadPhotoPayload(
        bytes = bytes,
        mimeType = mimeType,
        fileName = fileName,
    )
}
