package com.howmylook.app.data.upload

data class UploadUiState(
    val loading: Boolean = false,
    val occasion: String = "",
    val selectedPhotos: List<String> = emptyList(),
    val selectedPhotoNames: List<String> = emptyList(),
    val pickerLaunchNonce: Int = 0,
    val cameraLaunchNonce: Int = 0,
    val message: String = "",
    val error: String? = null,
    val lastCreatedPostId: String? = null,
)
