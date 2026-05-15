package com.howmylook.app.viewmodel

import android.content.ContentResolver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.auth.AuthBootstrapRepository
import com.howmylook.app.data.auth.AuthFormState
import com.howmylook.app.data.auth.AuthMode
import com.howmylook.app.data.auth.AuthRepository
import com.howmylook.app.data.auth.SessionState
import com.howmylook.app.data.auth.UsernameFormState
import com.howmylook.app.data.feed.FeedRepository
import com.howmylook.app.data.feed.HomeDestination
import com.howmylook.app.data.feed.HomeUiState
import com.howmylook.app.data.feed.RatingCard
import com.howmylook.app.data.post.FollowListUiState
import com.howmylook.app.data.post.PostDetailUiState
import com.howmylook.app.data.post.PostRepository
import com.howmylook.app.data.profile.FollowListRepository
import com.howmylook.app.data.profile.PeopleRepository
import com.howmylook.app.data.profile.ProfileRepository
import com.howmylook.app.data.profile.ProfileUiState
import com.howmylook.app.data.search.ExploreLookCard
import com.howmylook.app.data.search.ExploreProfileCard
import com.howmylook.app.data.search.SearchRepository
import com.howmylook.app.data.search.SearchUiState
import com.howmylook.app.data.upload.UploadRepository
import com.howmylook.app.data.upload.UploadUiState
import com.howmylook.app.domain.AppConfig
import com.howmylook.app.domain.AppRoute
import com.howmylook.app.domain.AppStep
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val authBootstrapRepository = AuthBootstrapRepository()
    private val feedRepository = FeedRepository()
    private val profileRepository = ProfileRepository()
    private val peopleRepository = PeopleRepository()
    private val followListRepository = FollowListRepository()
    private val postRepository = PostRepository()
    private val searchRepository = SearchRepository()
    private val uploadRepository = UploadRepository()
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

    var selectedPersonProfileId by mutableStateOf<String?>(null)
        private set

    var currentUserId: String? by mutableStateOf(null)
        private set

    var bootstrapMessage by mutableStateOf("Bootstrapping…")
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
                }
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
                        bootstrapSession()
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
        viewModelScope.launch {
            profileRepository.loadOwnProfile(supabaseConfig, userId)
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
            searchRepository.loadSearch(supabaseConfig, currentUserId)
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

    fun requestUploadPhotoPicker() {
        uploadUiState = uploadUiState.copy(
            pickerLaunchNonce = uploadUiState.pickerLaunchNonce + 1,
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

    fun openPostDetail(postId: String) {
        viewModelScope.launch {
            postDetailUiState = postDetailUiState.copy(loading = true, error = null)
            postRepository.loadPostDetail(supabaseConfig, postId)
                .onSuccess { state ->
                    postDetailUiState = state
                }
                .onFailure { error ->
                    postDetailUiState = postDetailUiState.copy(
                        loading = false,
                        error = error.message ?: "Unable to load post.",
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

    fun openYesGiven() {
        followListUiState = FollowListUiState(
            loading = false,
            title = "Yes given",
            people = emptyList(),
            error = "Detailed Yes-given history is not wired yet in native."
        )
    }

    fun openNoGiven() {
        followListUiState = FollowListUiState(
            loading = false,
            title = "No given",
            people = emptyList(),
            error = "Detailed No-given history is not wired yet in native."
        )
    }

    fun startEditProfile() {
        profileUiState = profileUiState.copy(
            error = "Profile editing entry is added, but the native edit form is the next step."
        )
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
                    val nextMessage = if (nextUnlockVotes >= AppConfig.unlockVoteCount) {
                        "Unlock complete. Home is ready."
                    } else {
                        "${if (value == "yes") "Yes" else "No"} saved. ${AppConfig.unlockVoteCount - nextUnlockVotes} ratings left."
                    }
                    bootstrapMessage = nextMessage
                    sessionState = sessionState.copy(
                        unlockVotesCompleted = nextUnlockVotes,
                        bootstrapMessage = nextMessage,
                    )
                    homeUiState = homeUiState.copy(
                        isLoading = false,
                        destination = if (nextUnlockVotes >= AppConfig.unlockVoteCount) HomeDestination.UNLOCKED_HOME else HomeDestination.LOCKED_HOME,
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
