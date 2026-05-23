package com.howmylook.app.viewmodel

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.activity.ActivityRepository
import com.howmylook.app.data.activity.ActivityUiState
import com.howmylook.app.data.auth.AuthBootstrapRepository
import com.howmylook.app.data.auth.AuthFormState
import com.howmylook.app.data.auth.AuthMode
import com.howmylook.app.data.auth.AuthRepository
import com.howmylook.app.data.auth.SessionState
import com.howmylook.app.data.auth.UsernameFormState
import com.howmylook.app.data.feed.FeedRepository
import com.howmylook.app.data.notifications.AndroidPushRepository
import com.howmylook.app.data.notifications.NotificationPermissionState
import com.howmylook.app.data.feed.HomeDestination
import com.howmylook.app.data.feed.HomeUiState
import com.howmylook.app.data.feed.RatingCard
import com.howmylook.app.data.post.FollowListUiState
import com.howmylook.app.data.post.PostDetailUiState
import com.howmylook.app.data.post.EditPostRepository
import com.howmylook.app.data.post.PostRepository
import com.howmylook.app.data.profile.EditProfileFormState
import com.howmylook.app.data.profile.EditProfileRepository
import com.howmylook.app.data.profile.FollowListRepository
import com.howmylook.app.data.profile.NotificationSettingsRepository
import com.howmylook.app.data.profile.PeopleRepository
import com.howmylook.app.data.profile.ProfileRepository
import com.howmylook.app.data.profile.ProfileUiState
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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val authBootstrapRepository = AuthBootstrapRepository()
    private val feedRepository = FeedRepository()
    private val activityRepository = ActivityRepository()
    private val profileRepository = ProfileRepository()
    private val peopleRepository = PeopleRepository()
    private val followListRepository = FollowListRepository()
    private val voteHistoryRepository = VoteHistoryRepository()
    private val editProfileRepository = EditProfileRepository()
    private val notificationSettingsRepository = NotificationSettingsRepository()
    private val postRepository = PostRepository()
    private val editPostRepository = EditPostRepository()
    private val searchRepository = SearchRepository()
    private val uploadRepository = UploadRepository()
    private val androidPushRepository = AndroidPushRepository()
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
                    bootstrapSession()
                }
                .onFailure { error ->
                    profileUiState = profileUiState.copy(error = error.message ?: "Unable to log out.")
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
                    searchUiState = state
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
        submitVote("yes")
    }

    fun voteNo() {
        submitVote("no")
    }

    fun updateUploadOccasion(value: String) {
        uploadUiState = uploadUiState.copy(occasion = value, error = null)
    }

    fun updateSearchQuery(value: String) {
        searchUiState = searchUiState.copy(query = value)
        loadSearch()
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
        val limited = photoUris.take(5)
        uploadUiState = uploadUiState.copy(
            selectedPhotos = limited,
            selectedPhotoNames = limited.mapIndexed { index, _ -> "Photo ${index + 1}" },
            message = if (limited.isEmpty()) "" else "${limited.size} photo${if (limited.size == 1) "" else "s"} selected from device.",
            error = null,
        )
    }

    fun openPersonProfile(profileId: String) {
        selectedPersonProfileId = profileId
        val userId = currentUserId ?: return
        viewModelScope.launch {
            profileUiState = profileUiState.copy(loading = true, error = null)
            peopleRepository.loadPersonProfile(supabaseConfig, userId, profileId)
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

    fun openPostDetail(postId: String, fromRoute: String = "") {
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null, fromRoute = fromRoute)
            postRepository.loadPostDetail(supabaseConfig, postId, currentUserId)
                .onSuccess { state ->
                    postDetailUiState = state.copy(fromRoute = fromRoute)
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
            voteHistoryUiState = voteHistoryUiState.copy(loading = true, title = "Yes given", error = null)
            voteHistoryRepository.load(supabaseConfig, userId, "yes")
                .onSuccess { state ->
                    voteHistoryUiState = state
                }
                .onFailure { error ->
                    voteHistoryUiState = voteHistoryUiState.copy(
                        loading = false,
                        title = "Yes given",
                        error = error.message ?: "Unable to load Yes history.",
                    )
                }
        }
    }

    fun openNoGiven() {
        val userId = selectedPersonProfileId ?: currentUserId ?: return
        viewModelScope.launch {
            voteHistoryUiState = voteHistoryUiState.copy(loading = true, title = "No given", error = null)
            voteHistoryRepository.load(supabaseConfig, userId, "no")
                .onSuccess { state ->
                    voteHistoryUiState = state
                }
                .onFailure { error ->
                    voteHistoryUiState = voteHistoryUiState.copy(
                        loading = false,
                        title = "No given",
                        error = error.message ?: "Unable to load No history.",
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

    fun registerPendingPushToken() {
        val userId = currentUserId ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNullOrBlank()) return@addOnSuccessListener
            viewModelScope.launch {
                androidPushRepository.registerToken(
                    config = supabaseConfig,
                    userId = userId,
                    token = token,
                    deviceName = "Android",
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

        if (uploadUiState.selectedPhotos.isEmpty()) {
            uploadUiState = uploadUiState.copy(error = "Add at least 1 photo before publishing.")
            return
        }

        viewModelScope.launch {
            uploadUiState = uploadUiState.copy(loading = true, error = null, message = "Publishing...")
            val photoPayloads = uploadUiState.selectedPhotos.map { uriString ->
                com.howmylook.app.data.upload.loadUploadPhotoPayload(contentResolver, uriString)
            }

            uploadRepository.createPendingPost(
                config = supabaseConfig,
                userId = userId,
                occasion = uploadUiState.occasion,
                photos = photoPayloads,
            )
                .onSuccess { postId ->
                    uploadUiState = UploadUiState(
                        loading = false,
                        message = "Post submitted for moderation. It should appear after approval.",
                        lastCreatedPostId = postId,
                    )
                    loadProfile()
                    loadSearch()
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
            homeUiState = homeUiState.copy(isLoading = true, statusMessage = "Saving vote...")
            feedRepository.castVote(supabaseConfig, card.id, value)
                .onSuccess { result ->
                    val nextUnlockVotes = result.loginRatingVotesCompleted
                        ?: result.unlockVotesCompleted
                        ?: (sessionState.unlockVotesCompleted + 1)

                    val nextQueue = ratingQueue.filterNot { it.id == card.id }
                    ratingQueue = nextQueue
                    currentCard = nextQueue.firstOrNull()
                    val requiredRatings = minOf(AppConfig.unlockVoteCount, sessionState.availablePostCount.coerceAtLeast(0))
                    val unlockedNow = nextUnlockVotes >= requiredRatings
                    val remaining = (requiredRatings - nextUnlockVotes).coerceAtLeast(0)
                    val nextMessage = if (unlockedNow) {
                        ""
                    } else {
                        "${if (value == "yes") "Yes" else "No"} saved. $remaining ratings left."
                    }
                    bootstrapMessage = nextMessage
                    sessionState = sessionState.copy(
                        needsUnlockRatings = !unlockedNow,
                        unlockVotesCompleted = nextUnlockVotes,
                        bootstrapMessage = nextMessage,
                    )
                    homeUiState = homeUiState.copy(
                        isLoading = false,
                        destination = if (unlockedNow) HomeDestination.UNLOCKED_HOME else HomeDestination.LOCKED_HOME,
                        statusMessage = nextMessage,
                    )
                    loadSearch()
                    loadProfile()
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to save vote."
                    bootstrapMessage = message
                    sessionState = sessionState.copy(bootstrapMessage = message)
                    homeUiState = homeUiState.copy(isLoading = false, statusMessage = message)
                }
        }
    }
}
