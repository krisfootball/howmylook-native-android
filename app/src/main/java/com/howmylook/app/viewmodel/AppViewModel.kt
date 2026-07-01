package com.howmylook.app.viewmodel

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.admin.AdminModerationRepository
import com.howmylook.app.data.admin.AdminUiState
import com.howmylook.app.data.activity.ActivityRepository
import com.howmylook.app.data.activity.ActivityUiState
import com.howmylook.app.data.auth.AuthBootstrapRepository
import com.howmylook.app.data.auth.PasswordResetRepository
import com.howmylook.app.data.auth.AuthFormState
import com.howmylook.app.data.auth.AuthMode
import com.howmylook.app.data.auth.AuthRepository
import com.howmylook.app.data.auth.SessionState
import com.howmylook.app.data.auth.UsernameFormState
import com.howmylook.app.data.feed.FeedRepository
import com.howmylook.app.data.notifications.AndroidPushRepository
import com.howmylook.app.data.notifications.NotificationPermissionState
import com.howmylook.app.data.notifications.PostNotificationRepository
import com.howmylook.app.data.feed.HomeDestination
import com.howmylook.app.data.feed.HomeUiState
import com.howmylook.app.data.feed.RatingCard
import com.howmylook.app.data.feed.VoteResultDto
import com.howmylook.app.data.post.FollowListUiState
import com.howmylook.app.data.post.PostDetailUiState
import com.howmylook.app.data.post.EditPostRepository
import com.howmylook.app.data.post.PostRepository
import com.howmylook.app.data.profile.AccountDeletionRepository
import com.howmylook.app.data.profile.EditProfileFormState
import com.howmylook.app.data.profile.EditProfileRepository
import com.howmylook.app.data.profile.FollowListRepository
import com.howmylook.app.data.profile.NotificationSettingsRepository
import com.howmylook.app.data.profile.PeopleRepository
import com.howmylook.app.data.profile.ProfileRepository
import com.howmylook.app.data.profile.ProfileUiState
import com.howmylook.app.data.reports.ContentReportRepository
import com.howmylook.app.data.profile.VoteHistoryRepository
import com.howmylook.app.data.profile.VoteHistoryUiState
import com.howmylook.app.data.search.ExploreLookCard
import com.howmylook.app.data.search.ExploreProfileCard
import com.howmylook.app.data.search.SearchRepository
import com.howmylook.app.data.search.SearchUiState
import com.howmylook.app.data.upload.UploadRepository
import com.howmylook.app.data.upload.loadUploadPhotoPayload
import com.howmylook.app.data.upload.UploadUiState
import com.howmylook.app.domain.AppConfig
import com.howmylook.app.domain.AppRoute
import com.howmylook.app.domain.AppStep
import com.howmylook.app.domain.resolveCompareVoteSide
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private const val MAX_POST_PHOTO_SIZE_BYTES = 10 * 1024 * 1024

class AppViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val authBootstrapRepository = AuthBootstrapRepository()
    private val passwordResetRepository = PasswordResetRepository()
    private val feedRepository = FeedRepository()
    private val activityRepository = ActivityRepository()
    private val profileRepository = ProfileRepository()
    private val peopleRepository = PeopleRepository()
    private val followListRepository = FollowListRepository()
    private val voteHistoryRepository = VoteHistoryRepository()
    private val editProfileRepository = EditProfileRepository()
    private val accountDeletionRepository = AccountDeletionRepository()
    private val contentReportRepository = ContentReportRepository()
    private val notificationSettingsRepository = NotificationSettingsRepository()
    private val postRepository = PostRepository()
    private val editPostRepository = EditPostRepository()
    private val searchRepository = SearchRepository()
    private val uploadRepository = UploadRepository()
    private val androidPushRepository = AndroidPushRepository()
    private val postNotificationRepository = PostNotificationRepository()
    private val adminModerationRepository = AdminModerationRepository()
    private val supabaseConfig = SupabaseConfig.fromBuildConfig()

    var sessionState by mutableStateOf(
        SessionState(
            isLoading = true,
            isSignedIn = false,
            needsUsername = false,
            needsUnlockRatings = true,
            unlockVotesCompleted = 0,
            availablePostCount = 0,
            bootstrapMessage = "Bootstrapping…",
        )
    )
        private set

    var ratingQueue by mutableStateOf<List<RatingCard>>(emptyList())
        private set

    var currentCard by mutableStateOf<RatingCard?>(null)
        private set

    var homeUiState by mutableStateOf(HomeUiState())
        private set

    var activityUiState by mutableStateOf(ActivityUiState())
        private set

    var authFormState by mutableStateOf(AuthFormState())
        private set

    var usernameFormState by mutableStateOf(UsernameFormState())
        private set

    var searchUiState by mutableStateOf(
        SearchUiState(
            looks = listOf(
                ExploreLookCard(id = "look-1", occasion = "Coffee run", yesCount = 14, noCount = 2, imageCount = 1),
                ExploreLookCard(id = "look-2", occasion = "Dinner date", yesCount = 20, noCount = 4, imageCount = 3),
                ExploreLookCard(id = "look-3", occasion = "Office day", yesCount = 9, noCount = 1, imageCount = 1),
            ),
            people = listOf(
                ExploreProfileCard(id = "person-1", displayName = "Sofia", username = "@sofiafits", bio = "Minimal outfits"),
                ExploreProfileCard(id = "person-2", displayName = "Luca", username = "@lucalooks", bio = "Streetwear"),
            ),
        )
    )
        private set

    var profileUiState by mutableStateOf(
        ProfileUiState(
            displayName = "Your profile",
            username = "@username",
            bio = "Your profile data will appear here once connected.",
        )
    )
        private set

    var uploadUiState by mutableStateOf(UploadUiState())
        private set

    var adminUiState by mutableStateOf(AdminUiState())
        private set

    var postDetailUiState by mutableStateOf(PostDetailUiState())
        private set

    var followListUiState by mutableStateOf(FollowListUiState())
        private set

    var voteHistoryUiState by mutableStateOf(VoteHistoryUiState())
        private set

    var editProfileFormState by mutableStateOf(EditProfileFormState())
        private set

    var selectedPersonProfileId by mutableStateOf<String?>(null)
        private set

    var currentUserId: String? by mutableStateOf(null)
        private set

    var bootstrapMessage by mutableStateOf("Bootstrapping…")
        private set

    var notificationPermissionState by mutableStateOf(NotificationPermissionState())
        private set

    var pendingNotificationPostId by mutableStateOf<String?>(null)
        private set

    init {
        bootstrapSession()
    }

    fun resolveStartRoute(): AppRoute {
        return when {
            sessionState.isLoading -> AppRoute.Splash
            !sessionState.isSignedIn -> AppRoute.Auth
            sessionState.needsUsername -> AppRoute.Username
            else -> AppRoute.Home
        }
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            sessionState = sessionState.copy(isLoading = true)
            val bootstrap = authBootstrapRepository.bootstrap(supabaseConfig)
            bootstrapMessage = bootstrap.message
            currentUserId = bootstrap.profile?.id
            usernameFormState = usernameFormState.copy(
                username = if (com.howmylook.app.domain.hasCompletedUsername(bootstrap.profile?.id, bootstrap.profile?.username)) {
                    bootstrap.profile?.username.orEmpty()
                } else {
                    ""
                },
                displayName = bootstrap.profile?.displayName.orEmpty(),
                loading = false,
                error = null,
            )
            profileUiState = profileUiState.copy(
                loading = true,
                displayName = bootstrap.profile?.displayName ?: "Your profile",
                username = bootstrap.profile?.username?.let { "@$it" } ?: "@username",
                bio = "Your profile data will appear here once connected.",
                error = null,
            )
            sessionState = sessionState.copy(
                isLoading = false,
                isSignedIn = bootstrap.isSignedIn,
                needsUsername = bootstrap.step == AppStep.USERNAME,
                needsUnlockRatings = bootstrap.step == AppStep.RATING,
                unlockVotesCompleted = bootstrap.profile?.loginRatingVotesCompleted ?: 0,
                availablePostCount = bootstrap.availablePostCount,
                isAdmin = bootstrap.profile?.isAdmin == true,
                bootstrapMessage = bootstrap.message,
                debugMessage = bootstrap.debugMessage,
            )

            homeUiState = homeUiState.copy(
                destination = if (bootstrap.step == AppStep.UNLOCKED) HomeDestination.UNLOCKED_HOME else HomeDestination.LOCKED_HOME,
                statusMessage = bootstrap.message,
            )

            if (bootstrap.isSignedIn) {
                if (bootstrap.step != AppStep.USERNAME) {
                    loadProfile()
                    loadSearch()
                    loadRatingQueue()
                    loadActivity()
                    if (bootstrap.profile?.isAdmin == true) {
                        loadAdminQueue()
                    }
                }
                registerPendingPushToken()
            }
        }
    }

    fun setAuthMode(mode: AuthMode) {
        authFormState = authFormState.copy(mode = mode, error = null, message = "")
    }

    fun updateEmail(email: String) {
        authFormState = authFormState.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        authFormState = authFormState.copy(password = password, error = null)
    }

    fun updateAcceptedPolicies(accepted: Boolean) {
        authFormState = authFormState.copy(acceptedPolicies = accepted, error = null)
    }

    fun submitAuth() {
        val email = authFormState.email.trim()
        val password = authFormState.password

        if (email.isBlank()) {
            authFormState = authFormState.copy(error = "Email is required.")
            return
        }

        if (password.length < 6) {
            authFormState = authFormState.copy(error = "Password must be at least 6 characters.")
            return
        }

        if (authFormState.mode == AuthMode.SIGN_UP && !authFormState.acceptedPolicies) {
            authFormState = authFormState.copy(error = "Please agree to the terms, privacy policy, and community guidelines.")
            return
        }

        viewModelScope.launch {
            authFormState = authFormState.copy(loading = true, error = null, message = "")
            bootstrapMessage = if (authFormState.mode == AuthMode.SIGN_IN) {
                "Signing in…"
            } else {
                "Creating account…"
            }

            val result = if (authFormState.mode == AuthMode.SIGN_UP) {
                authRepository.signUp(supabaseConfig, email, password)
            } else {
                authRepository.signIn(supabaseConfig, email, password)
                    .map { "Signed in." }
            }

            result
                .onSuccess { message ->
                    authFormState = authFormState.copy(loading = false, message = message, error = null)
                    bootstrapMessage = message
                    if (authFormState.mode == AuthMode.SIGN_UP && message.contains("Check your email")) {
                        setAuthMode(AuthMode.SIGN_IN)
                    } else {
                        viewModelScope.launch {
                            if (authFormState.mode == AuthMode.SIGN_IN) {
                                authRepository.loadCurrentProfile(supabaseConfig)
                                    .onSuccess { profile ->
                                        profile?.id?.let { userId ->
                                            authRepository.resetLoginRatingCounter(supabaseConfig, userId)
                                        }
                                    }
                            }
                            bootstrapSession()
                        }
                    }
                }
                .onFailure { error ->
                    authFormState = authFormState.copy(
                        loading = false,
                        error = com.howmylook.app.data.toFriendlyAuthError(error.message),
                    )
                }
        }
    }

    fun requestPasswordReset() {
        val email = authFormState.email.trim()
        if (email.isBlank()) {
            authFormState = authFormState.copy(error = "Enter your email first, then tap Forgot password.")
            return
        }

        viewModelScope.launch {
            authFormState = authFormState.copy(loading = true, error = null, message = "")
            passwordResetRepository.requestPasswordReset(supabaseConfig, email)
                .onSuccess { message ->
                    authFormState = authFormState.copy(loading = false, message = message, error = null)
                }
                .onFailure { error ->
                    authFormState = authFormState.copy(
                        loading = false,
                        error = error.message ?: "Unable to send password reset email.",
                    )
                }
        }
    }

    fun reportPost(postId: String, reason: String?) {
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, actionMessage = "")
            contentReportRepository.reportPost(supabaseConfig, postId, reason)
                .onSuccess { message ->
                    postDetailUiState = postDetailUiState.copy(loading = false, actionMessage = message, error = null)
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to submit report.",
                    )
                }
        }
    }

    fun reportProfile(profileId: String, reason: String?) {
        viewModelScope.launch {
            profileUiState = profileUiState.copy(loading = true, error = null, actionMessage = "")
            contentReportRepository.reportProfile(supabaseConfig, profileId, reason)
                .onSuccess { message ->
                    profileUiState = profileUiState.copy(loading = false, actionMessage = message, error = null)
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to submit report.",
                    )
                }
        }
    }

    fun updateUsername(username: String) {
        usernameFormState = usernameFormState.copy(username = username, error = null)
    }

    fun updateDisplayName(displayName: String) {
        usernameFormState = usernameFormState.copy(displayName = displayName, error = null)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut(supabaseConfig)
                .onSuccess {
                    clearSignedInState()
                    bootstrapSession()
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(error = error.message ?: "Unable to log out.")
                }
        }
    }

    private fun clearSignedInState() {
        currentUserId = null
        selectedPersonProfileId = null
        ratingQueue = emptyList()
        currentCard = null
        followListUiState = FollowListUiState()
        voteHistoryUiState = VoteHistoryUiState()
        editProfileFormState = EditProfileFormState()
        profileUiState = ProfileUiState()
        searchUiState = SearchUiState()
        activityUiState = ActivityUiState()
        uploadUiState = UploadUiState()
        postDetailUiState = PostDetailUiState()
        authFormState = authFormState.copy(password = "", message = "", error = null, loading = false)
    }

    fun requestAccountDeletion() {
        if (currentUserId == null) return

        viewModelScope.launch {
            editProfileFormState = editProfileFormState.copy(deleting = true, error = null, message = "")
            accountDeletionRepository.requestAccountDeletion(supabaseConfig)
                .onSuccess { message ->
                    editProfileFormState = editProfileFormState.copy(
                        deleting = false,
                        message = message,
                        error = null,
                    )
                }
                .onFailure { error ->
                    editProfileFormState = editProfileFormState.copy(
                        deleting = false,
                        error = error.message ?: "Unable to send account deletion email.",
                    )
                }
        }
    }

    fun submitUsername() {
        val userId = currentUserId ?: run {
            usernameFormState = usernameFormState.copy(error = "You need to sign in before choosing a username.")
            return
        }

        if (usernameFormState.username.trim().isBlank()) {
            usernameFormState = usernameFormState.copy(error = "Username is required.")
            return
        }

        viewModelScope.launch {
            usernameFormState = usernameFormState.copy(loading = true, error = null, message = "")
            authRepository.saveUsername(
                config = supabaseConfig,
                userId = userId,
                username = usernameFormState.username,
                displayName = usernameFormState.displayName,
            )
                .onSuccess {
                    usernameFormState = usernameFormState.copy(
                        loading = false,
                        message = "Profile saved.",
                        error = null,
                    )
                    bootstrapSession()
                }
                .onFailure { error ->
                    usernameFormState = usernameFormState.copy(
                        loading = false,
                        error = error.message ?: "Unable to save username.",
                    )
                }
        }
    }

    private fun loadProfile() {
        val userId = currentUserId ?: return
        val selectedProfileId = selectedPersonProfileId
        viewModelScope.launch {
            val result = if (selectedProfileId != null) {
                peopleRepository.loadPersonProfile(supabaseConfig, userId, selectedProfileId)
            } else {
                profileRepository.loadOwnProfile(supabaseConfig, userId)
            }

            result
                .onSuccess { state ->
                    profileUiState = state
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load profile.",
                    )
                }
        }
    }

    private fun loadSearch() {
        viewModelScope.launch {
            searchUiState = searchUiState.copy(loading = true, error = null)
            searchRepository.loadSearch(supabaseConfig, currentUserId, searchUiState.query)
                .onSuccess { state ->
                    searchUiState = state.copy(query = searchUiState.query)
                }
                .onFailure { error ->
                    searchUiState = searchUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load search.",
                    )
                }
        }
    }

    private fun loadRatingQueue() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            homeUiState = homeUiState.copy(isLoading = true)
            feedRepository.loadRatingQueue(supabaseConfig, userId)
                .onSuccess { queue ->
                    ratingQueue = queue
                    currentCard = queue.firstOrNull()
                    homeUiState = homeUiState.copy(
                        isLoading = false,
                        statusMessage = if (queue.isEmpty()) {
                            if (sessionState.unlockVotesCompleted >= AppConfig.unlockVoteCount) {
                                "Nice — you finished the required ratings. You can keep exploring the unlocked parts of the app now."
                            } else {
                                "You’ve gone through the available queue. New posts that still need their first 5 ratings will appear here first."
                            }
                        } else {
                            homeUiState.statusMessage
                        },
                    )
                }
                .onFailure { error ->
                    bootstrapMessage = error.message ?: "Unable to load rating queue."
                    ratingQueue = emptyList()
                    currentCard = null
                    homeUiState = homeUiState.copy(
                        isLoading = false,
                        statusMessage = error.message ?: "Unable to load rating queue.",
                    )
                }
        }
    }

    fun voteYes() {
        val card = currentCard ?: return
        submitVote(if (card.postKind == "compare") "right" else "yes")
    }

    fun voteNo() {
        val card = currentCard ?: return
        submitVote(if (card.postKind == "compare") "left" else "no")
    }

    fun voteOnPostDetail(pickYes: Boolean) {
        val detail = postDetailUiState
        val postId = detail.postId ?: return
        if (detail.isOwnPost || detail.hasViewerVoted || detail.loading) return
        val value = when {
            detail.postKind == "compare" -> if (pickYes) "right" else "left"
            else -> if (pickYes) "yes" else "no"
        }
        submitVoteOnPost(postId, value, detail.postKind)
    }

    fun updateUploadPostKind(value: String) {
        val trimmedPhotos = if (value == "compare") uploadUiState.selectedPhotos.take(2) else uploadUiState.selectedPhotos.take(5)
        val trimmedNames = if (value == "compare") uploadUiState.selectedPhotoNames.take(2) else uploadUiState.selectedPhotoNames.take(5)
        uploadUiState = uploadUiState.copy(
            postKind = value,
            selectedPhotos = trimmedPhotos,
            selectedPhotoNames = trimmedNames,
            error = null,
            message = "",
        )
    }

    fun updateUploadOccasion(value: String) {
        uploadUiState = uploadUiState.copy(occasion = value, error = null)
    }

    fun updateSearchQuery(value: String) {
        searchUiState = searchUiState.copy(query = value)
    }

    fun requestUploadPhotoPicker() {
        uploadUiState = uploadUiState.copy(
            pickerLaunchNonce = uploadUiState.pickerLaunchNonce + 1,
            error = null,
            message = "",
        )
    }

    fun requestUploadCameraCapture() {
        uploadUiState = uploadUiState.copy(
            cameraLaunchNonce = uploadUiState.cameraLaunchNonce + 1,
            error = null,
            message = "",
        )
    }

    fun setSelectedUploadPhotos(photoUris: List<String>) {
        val limit = if (uploadUiState.postKind == "compare") 2 else 5
        val limited = photoUris.take(limit)
        uploadUiState = uploadUiState.copy(
            selectedPhotos = limited,
            selectedPhotoNames = limited.mapIndexed { index, _ -> "Photo ${index + 1}" },
            message = if (limited.isEmpty()) "" else "${limited.size} photo${if (limited.size == 1) "" else "s"} selected from device.",
            error = null,
        )
    }

    fun addCameraUploadPhoto(photoUri: String) {
        val limit = if (uploadUiState.postKind == "compare") 2 else 5
        val currentPhotos = uploadUiState.selectedPhotos
        val updatedPhotos = if (currentPhotos.size >= limit) {
            currentPhotos.dropLast(1) + photoUri
        } else {
            currentPhotos + photoUri
        }
        val photoNames = if (uploadUiState.postKind == "compare") {
            updatedPhotos.mapIndexed { index, _ ->
                if (index == 0) "Left photo" else "Right photo"
            }
        } else {
            updatedPhotos.mapIndexed { index, _ -> "Photo ${index + 1}" }
        }
        val message = when {
            uploadUiState.postKind == "compare" && updatedPhotos.size == 1 ->
                "Left photo captured. Tap the camera again for the right photo."
            uploadUiState.postKind == "compare" && updatedPhotos.size == 2 ->
                "Both split photos captured."
            updatedPhotos.size == 1 -> "Photo captured from camera."
            else -> "${updatedPhotos.size} photos captured from camera."
        }
        uploadUiState = uploadUiState.copy(
            selectedPhotos = updatedPhotos,
            selectedPhotoNames = photoNames,
            message = message,
            error = null,
        )
    }

    fun openPersonProfile(profileId: String) {
        selectedPersonProfileId = profileId
        val userId = currentUserId ?: return
        viewModelScope.launch {
            profileUiState = profileUiState.copy(
                loading = true,
                error = null,
                isOwnProfile = false,
                profileId = profileId,
            )
            peopleRepository.loadPersonProfile(supabaseConfig, userId, profileId)
                .onSuccess { state ->
                    profileUiState = state
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(
                        loading = false,
                        isOwnProfile = false,
                        profileId = profileId,
                        error = error.message ?: "Unable to load profile.",
                    )
                }
        }
    }

    fun openOwnProfile() {
        selectedPersonProfileId = null
        loadProfile()
    }

    fun openSelectedOwnerProfile() {
        val profileId = selectedPersonProfileId ?: return
        openPersonProfile(profileId)
    }

    fun followSelectedProfile() {
        val viewerUserId = currentUserId ?: return
        val profileId = selectedPersonProfileId ?: return
        val shouldFollow = !profileUiState.isFollowing
        viewModelScope.launch {
            profileUiState = profileUiState.copy(loading = true, error = null)
            peopleRepository.setFollowing(supabaseConfig, viewerUserId, profileId, shouldFollow)
                .onSuccess {
                    openPersonProfile(profileId)
                    loadSearch()
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to update following.",
                    )
                }
        }
    }

    fun openPostDetail(postId: String, fromRoute: String = "", selectedCompareSide: String? = null) {
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, fromRoute = fromRoute)
            postRepository.loadPostDetail(supabaseConfig, postId, currentUserId)
                .onSuccess { state ->
                    postDetailUiState = state.copy(
                        fromRoute = fromRoute,
                        selectedCompareSide = selectedCompareSide ?: state.selectedCompareSide,
                    )
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load post.",
                    )
                }
        }
    }

    fun openPostAuthorProfile() {
        val ownerId = postDetailUiState.ownerId ?: return
        openPersonProfile(ownerId)
    }



    fun editCurrentPostOccasion(occasion: String) {
        val ownerUserId = currentUserId ?: return
        val postId = postDetailUiState.postId ?: return
        if (!postDetailUiState.isOwnPost) return

        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, actionMessage = "")
            editPostRepository.updateOccasion(supabaseConfig, postId, ownerUserId, occasion)
                .onSuccess { message ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        occasion = occasion.ifBlank { "No occasion added yet" },
                        actionMessage = message,
                        error = null,
                    )
                    loadProfile()
                    loadSearch()
                    loadRatingQueue()
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to update photo.",
                    )
                }
        }
    }

    fun deleteCurrentPost() {
        val ownerUserId = currentUserId ?: return
        val postId = postDetailUiState.postId ?: return
        if (!postDetailUiState.isOwnPost) return

        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, actionMessage = "")
            postRepository.deleteOwnPost(supabaseConfig, postId, ownerUserId)
                .onSuccess { message ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        actionMessage = message,
                        error = null,
                    )
                    loadProfile()
                    loadSearch()
                    loadRatingQueue()
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to delete photo.",
                    )
                }
        }
    }

    fun toggleKeepCurrentPost() {
        val ownerUserId = currentUserId ?: return
        val postId = postDetailUiState.postId ?: return
        if (!postDetailUiState.isOwnPost) return

        val nextKeepState = !postDetailUiState.keepForever
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, actionMessage = "")
            postRepository.toggleKeepForever(supabaseConfig, postId, ownerUserId, nextKeepState)
                .onSuccess { message ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        keepForever = nextKeepState,
                        actionMessage = message,
                        error = null,
                    )
                    loadProfile()
                    loadSearch()
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to update keep setting.",
                    )
                }
        }
    }

    fun openFollowers() {
        val profileId = profileUiState.profileId ?: currentUserId ?: return
        viewModelScope.launch {
            followListUiState = followListUiState.copy(loading = true, error = null)
            followListRepository.loadFollowers(supabaseConfig, profileId)
                .onSuccess { state ->
                    followListUiState = state
                }
                .onFailure { error ->
                    followListUiState = followListUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load followers.",
                    )
                }
        }
    }

    fun openFollowing() {
        val profileId = profileUiState.profileId ?: currentUserId ?: return
        viewModelScope.launch {
            followListUiState = followListUiState.copy(loading = true, error = null)
            followListRepository.loadFollowing(supabaseConfig, profileId)
                .onSuccess { state ->
                    followListUiState = state
                }
                .onFailure { error ->
                    followListUiState = followListUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load following.",
                    )
                }
        }
    }

    fun loadActivity() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            activityUiState = activityUiState.copy(loading = true, error = null)
            activityRepository.load(supabaseConfig, userId)
                .onSuccess { state ->
                    activityUiState = state
                }
                .onFailure { error ->
                    activityUiState = activityUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load activity.",
                    )
                }
        }
    }

    fun openYesGiven() {
        val userId = selectedPersonProfileId ?: currentUserId ?: return
        viewModelScope.launch {
            voteHistoryUiState = voteHistoryUiState.copy(loading = true, title = "Liked", error = null)
            voteHistoryRepository.load(supabaseConfig, userId, "yes")
                .onSuccess { state ->
                    voteHistoryUiState = state
                }
                .onFailure { error ->
                    voteHistoryUiState = voteHistoryUiState.copy(
                        loading = false,
                        title = "Liked",
                        error = error.message ?: "Unable to load Yes history.",
                    )
                }
        }
    }

    fun openNoGiven() {
        val userId = selectedPersonProfileId ?: currentUserId ?: return
        viewModelScope.launch {
            voteHistoryUiState = voteHistoryUiState.copy(loading = true, title = "Skipped", error = null)
            voteHistoryRepository.load(supabaseConfig, userId, "no")
                .onSuccess { state ->
                    voteHistoryUiState = state
                }
                .onFailure { error ->
                    voteHistoryUiState = voteHistoryUiState.copy(
                        loading = false,
                        title = "Skipped",
                        error = error.message ?: "Unable to load No history.",
                    )
                }
        }
    }


    fun openPickedGiven() {
        val userId = selectedPersonProfileId ?: currentUserId ?: return
        viewModelScope.launch {
            voteHistoryUiState = voteHistoryUiState.copy(loading = true, title = "Picked", error = null)
            voteHistoryRepository.load(supabaseConfig, userId, "picked")
                .onSuccess { state ->
                    voteHistoryUiState = state
                }
                .onFailure { error ->
                    voteHistoryUiState = voteHistoryUiState.copy(
                        loading = false,
                        title = "Picked",
                        error = error.message ?: "Unable to load Picked history.",
                    )
                }
        }
    }

    fun startEditProfile() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            editProfileFormState = editProfileFormState.copy(loading = true, saving = false, error = null, message = "")
            editProfileRepository.load(supabaseConfig, userId)
                .onSuccess { state ->
                    editProfileFormState = state
                }
                .onFailure { error ->
                    editProfileFormState = editProfileFormState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load profile editor.",
                    )
                }
        }
    }

    fun refreshNotificationPermissionState(context: android.content.Context) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        notificationPermissionState = notificationPermissionState.copy(
            supported = true,
            granted = granted,
        )
    }

    fun markNotificationPermissionRequested() {
        notificationPermissionState = notificationPermissionState.copy(requestedThisSession = true)
    }

    fun handleNotificationOpen(postId: String) {
        if (postId.isBlank()) return
        pendingNotificationPostId = postId
    }

    fun clearPendingNotificationPostId() {
        pendingNotificationPostId = null
    }

    fun clearLastCreatedPostId() {
        uploadUiState = uploadUiState.copy(lastCreatedPostId = null)
    }

    fun registerPendingPushToken(context: Context? = null) {
        val userId = currentUserId ?: return
        val pendingFromStorage = context
            ?.getSharedPreferences(PUSH_PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(PENDING_FCM_TOKEN_KEY, null)
            ?.takeIf { it.isNotBlank() }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { currentToken ->
            val token = pendingFromStorage ?: currentToken
            if (token.isNullOrBlank()) return@addOnSuccessListener
            viewModelScope.launch {
                androidPushRepository.registerToken(
                    config = supabaseConfig,
                    userId = userId,
                    token = token,
                    deviceName = "Android",
                ).onSuccess {
                    context?.getSharedPreferences(PUSH_PREFS_NAME, Context.MODE_PRIVATE)
                        ?.edit()
                        ?.remove(PENDING_FCM_TOKEN_KEY)
                        ?.apply()
                }
            }
        }
    }

    private fun notifyFollowersAboutPost(postId: String) {
        viewModelScope.launch {
            postNotificationRepository.notifyFollowers(supabaseConfig, postId)
        }
    }

    fun loadAdminQueue() {
        if (!sessionState.isAdmin) return
        viewModelScope.launch {
            adminUiState = adminUiState.copy(loading = true, error = null)
            adminModerationRepository.loadPendingPosts(supabaseConfig)
                .onSuccess { posts ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        posts = posts,
                        error = null,
                    )
                }
                .onFailure { error ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load admin queue.",
                    )
                }
        }
    }

    fun approveAdminPost(postId: String) {
        if (!sessionState.isAdmin) return
        viewModelScope.launch {
            adminUiState = adminUiState.copy(loading = true, error = null, actionMessage = "")
            adminModerationRepository.approvePost(supabaseConfig, postId)
                .onSuccess { message ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        posts = adminUiState.posts.filterNot { it.id == postId },
                        actionMessage = message,
                    )
                    loadSearch()
                    loadRatingQueue()
                }
                .onFailure { error ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to approve post.",
                    )
                }
        }
    }

    fun deleteAdminPost(postId: String) {
        if (!sessionState.isAdmin) return
        viewModelScope.launch {
            adminUiState = adminUiState.copy(loading = true, error = null, actionMessage = "")
            adminModerationRepository.deletePost(supabaseConfig, postId)
                .onSuccess { message ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        posts = adminUiState.posts.filterNot { it.id == postId },
                        actionMessage = message,
                    )
                    loadSearch()
                    loadRatingQueue()
                    loadProfile()
                }
                .onFailure { error ->
                    adminUiState = adminUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to delete post.",
                    )
                }
        }
    }

    fun toggleProfileNotifications() {
        val viewerUserId = currentUserId ?: return
        val profileId = selectedPersonProfileId ?: return
        val nextEnabled = !profileUiState.notificationsEnabled
        viewModelScope.launch {
            profileUiState = profileUiState.copy(loading = true, error = null)
            notificationSettingsRepository.setEnabled(supabaseConfig, viewerUserId, profileId, nextEnabled)
                .onSuccess {
                    profileUiState = profileUiState.copy(
                        loading = false,
                        notificationsEnabled = nextEnabled,
                        error = null,
                    )
                    if (nextEnabled) {
                        registerPendingPushToken()
                    }
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to update notification settings.",
                    )
                }
        }
    }

    fun updateEditUsername(value: String) {
        editProfileFormState = editProfileFormState.copy(username = value, error = null, message = "")
    }

    fun updateEditDisplayName(value: String) {
        editProfileFormState = editProfileFormState.copy(displayName = value, error = null, message = "")
    }

    fun updateEditBio(value: String) {
        editProfileFormState = editProfileFormState.copy(bio = value, error = null, message = "")
    }

    fun setEditAvatar(uriString: String?, displayName: String? = null) {
        editProfileFormState = editProfileFormState.copy(
            selectedAvatarUri = uriString,
            selectedAvatarName = displayName,
            removeAvatar = false,
            error = null,
            message = if (uriString.isNullOrBlank()) "" else "Selected ${displayName ?: "new photo"}",
        )
    }

    fun markEditAvatarForRemoval() {
        editProfileFormState = editProfileFormState.copy(
            selectedAvatarUri = null,
            selectedAvatarName = null,
            removeAvatar = true,
            error = null,
            message = "Profile photo will be removed when you save.",
        )
    }

    fun cancelEditAvatarRemoval() {
        editProfileFormState = editProfileFormState.copy(
            removeAvatar = false,
            error = null,
            message = "Photo removal canceled.",
        )
    }

    fun submitEditProfile(contentResolver: ContentResolver) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            editProfileFormState = editProfileFormState.copy(saving = true, error = null, message = "")
            editProfileRepository.save(
                config = supabaseConfig,
                userId = userId,
                username = editProfileFormState.username,
                displayName = editProfileFormState.displayName,
                bio = editProfileFormState.bio,
                avatarUrl = editProfileFormState.avatarUrl,
                selectedAvatarUri = editProfileFormState.selectedAvatarUri,
                removeAvatar = editProfileFormState.removeAvatar,
                contentResolver = contentResolver,
            )
                .onSuccess { message ->
                    editProfileFormState = editProfileFormState.copy(saving = false, message = message, error = null)
                    bootstrapSession()
                }
                .onFailure { error ->
                    editProfileFormState = editProfileFormState.copy(
                        saving = false,
                        error = error.message ?: "Unable to update profile.",
                    )
                }
        }
    }

    fun submitUpload(contentResolver: ContentResolver) {
        val userId = currentUserId ?: run {
            uploadUiState = uploadUiState.copy(error = "You need to sign in before posting.")
            return
        }

        if (uploadUiState.postKind == "compare") {
            if (uploadUiState.selectedPhotos.size != 2) {
                uploadUiState = uploadUiState.copy(error = "Compare posts need exactly 2 photos.")
                return
            }
        } else if (uploadUiState.selectedPhotos.isEmpty()) {
            uploadUiState = uploadUiState.copy(error = "Add at least 1 photo before publishing.")
            return
        }
        if (uploadUiState.occasion.trim().isBlank()) {
            uploadUiState = uploadUiState.copy(error = "Add an occasion before publishing.")
            return
        }

        viewModelScope.launch {
            uploadUiState = uploadUiState.copy(loading = true, error = null, message = "Publishing...")
            val photoPayloads = uploadUiState.selectedPhotos.map { uriString ->
                com.howmylook.app.data.upload.loadUploadPhotoPayload(contentResolver, uriString)
            }
            val oversizedPhoto = photoPayloads.firstOrNull { it.bytes.size > MAX_POST_PHOTO_SIZE_BYTES }
            if (oversizedPhoto != null) {
                uploadUiState = uploadUiState.copy(
                    loading = false,
                    error = "Each post photo must be 10 MB or smaller.",
                    message = "",
                )
                return@launch
            }

            uploadRepository.createPendingPost(
                config = supabaseConfig,
                userId = userId,
                occasion = uploadUiState.occasion,
                photos = photoPayloads,
                postKind = uploadUiState.postKind,
            )
                .onSuccess { postId ->
                    uploadUiState = UploadUiState(lastCreatedPostId = postId)
                    notifyFollowersAboutPost(postId)
                    if (sessionState.isAdmin) {
                        loadAdminQueue()
                    }
                    loadProfile()
                    loadSearch()
                    loadRatingQueue()
                }
                .onFailure { error ->
                    uploadUiState = uploadUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to create post.",
                        message = "",
                    )
                }
        }
    }

    private fun submitVote(value: String) {
        val card = currentCard ?: return
        if (homeUiState.isLoading) return

        viewModelScope.launch {
            homeUiState = homeUiState.copy(
                isLoading = true,
                statusMessage = if (card.postKind == "compare") "Saving pick..." else "Saving vote...",
                compareSelection = when (value) {
                    "left" -> "left"
                    "right" -> "right"
                    else -> null
                },
            )
            feedRepository.castVote(supabaseConfig, card.id, value)
                .onSuccess { result ->
                    val unlockMessage = applyVoteUnlockProgress(result, value)
                    removePostFromRatingQueue(card.id)
                    val nextQueue = ratingQueue
                    currentCard = nextQueue.firstOrNull()
                    homeUiState = homeUiState.copy(
                        isLoading = false,
                        destination = if (sessionState.needsUnlockRatings) HomeDestination.LOCKED_HOME else HomeDestination.UNLOCKED_HOME,
                        statusMessage = unlockMessage,
                        compareSelection = null,
                    )
                    loadSearch()
                    loadProfile()
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to save vote."
                    bootstrapMessage = message
                    sessionState = sessionState.copy(bootstrapMessage = message)
                    homeUiState = homeUiState.copy(isLoading = false, statusMessage = message, compareSelection = null)
                }
        }
    }

    private fun submitVoteOnPost(postId: String, value: String, postKind: String) {
        val fromRoute = postDetailUiState.fromRoute
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(
                loading = true,
                error = null,
                actionMessage = if (postKind == "compare") "Saving pick..." else "Saving vote...",
            )
            feedRepository.castVote(supabaseConfig, postId, value)
                .onSuccess { result ->
                    applyVoteUnlockProgress(result, value)
                    removePostFromRatingQueue(postId)
                    val pickedSide = if (postKind == "compare") {
                        resolveCompareVoteSide(value, "compare")
                            ?: if (value == "left") "left" else "right"
                    } else {
                        postDetailUiState.selectedCompareSide
                    }
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        hasViewerVoted = true,
                        selectedCompareSide = pickedSide,
                        yesCount = result.yesCount ?: postDetailUiState.yesCount,
                        noCount = result.noCount ?: postDetailUiState.noCount,
                        compareLeftPickCount = result.compareLeftPickCount ?: postDetailUiState.compareLeftPickCount,
                        compareRightPickCount = result.compareRightPickCount ?: postDetailUiState.compareRightPickCount,
                        actionMessage = postDetailVoteMessage(value),
                        error = null,
                        fromRoute = fromRoute,
                    )
                    loadSearch()
                    loadProfile()
                    loadRatingQueue()
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        actionMessage = "",
                        error = error.message ?: "Unable to save vote.",
                    )
                }
        }
    }

    private fun removePostFromRatingQueue(postId: String) {
        if (ratingQueue.none { it.id == postId }) return
        ratingQueue = ratingQueue.filterNot { it.id == postId }
    }

    private fun applyVoteUnlockProgress(result: VoteResultDto, value: String): String {
        val nextUnlockVotes = result.loginRatingVotesCompleted
            ?: result.unlockVotesCompleted
            ?: (sessionState.unlockVotesCompleted + 1)

        val requiredRatings = minOf(AppConfig.unlockVoteCount, sessionState.availablePostCount.coerceAtLeast(0))
        val unlockedNow = nextUnlockVotes >= requiredRatings
        val remaining = (requiredRatings - nextUnlockVotes).coerceAtLeast(0)
        val nextMessage = if (unlockedNow) {
            ""
        } else {
            when (value) {
                "yes" -> "Liked saved. $remaining ratings left."
                "no" -> "Skipped saved. $remaining ratings left."
                "left" -> "Picked left. $remaining ratings left."
                "right" -> "Picked right. $remaining ratings left."
                else -> "Saved. $remaining ratings left."
            }
        }
        bootstrapMessage = nextMessage
        sessionState = sessionState.copy(
            needsUnlockRatings = !unlockedNow,
            unlockVotesCompleted = nextUnlockVotes,
            bootstrapMessage = nextMessage,
        )
        return nextMessage
    }

    private fun postDetailVoteMessage(value: String): String {
        return when (value) {
            "yes" -> "${AppConfig.likeLabel} saved."
            "no" -> "${AppConfig.skipLabel} saved."
            "left" -> "Left pick saved."
            "right" -> "Right pick saved."
            else -> "Vote saved."
        }
    }

    companion object {
        private const val PUSH_PREFS_NAME = "howmylook_push"
        private const val PENDING_FCM_TOKEN_KEY = "pending_fcm_token"
    }
}
