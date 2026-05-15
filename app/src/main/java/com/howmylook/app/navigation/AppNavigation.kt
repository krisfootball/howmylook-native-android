package com.howmylook.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.howmylook.app.ui.screens.AuthScreen
import com.howmylook.app.ui.screens.FollowListScreen
import com.howmylook.app.ui.screens.HomeScreen
import com.howmylook.app.ui.screens.PostDetailScreen
import com.howmylook.app.ui.screens.ProfileScreen
import com.howmylook.app.ui.screens.SearchScreen
import com.howmylook.app.ui.screens.SplashScreen
import com.howmylook.app.ui.screens.UploadScreen
import com.howmylook.app.ui.screens.UsernameScreen
import com.howmylook.app.viewmodel.AppViewModel

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val startDestination = viewModel.resolveStartRoute().name
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            viewModel.setSelectedUploadPhotos(uris.map { it.toString() })
        },
    )

    LaunchedEffect(viewModel.uploadUiState.pickerLaunchNonce) {
        if (viewModel.uploadUiState.pickerLaunchNonce > 0) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    LaunchedEffect(
        viewModel.sessionState.isLoading,
        viewModel.sessionState.isSignedIn,
        viewModel.sessionState.needsUsername,
    ) {
        val routeName = viewModel.resolveStartRoute().name
        if (navController.currentDestination?.route != routeName) {
            navController.navigate(routeName) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("Splash") { SplashScreen() }
        composable("Auth") {
            AuthScreen(
                state = viewModel.authFormState,
                bootstrapMessage = viewModel.bootstrapMessage,
                debugMessage = viewModel.sessionState.debugMessage,
                onModeChange = viewModel::setAuthMode,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onAcceptedPoliciesChange = viewModel::updateAcceptedPolicies,
                onSubmit = viewModel::submitAuth,
            )
        }
        composable("Username") {
            UsernameScreen(
                state = viewModel.usernameFormState,
                onUsernameChange = viewModel::updateUsername,
                onDisplayNameChange = viewModel::updateDisplayName,
                onSubmit = viewModel::submitUsername,
            )
        }
        composable("Home") {
            HomeScreen(
                sessionState = viewModel.sessionState,
                card = viewModel.currentCard,
                homeUiState = viewModel.homeUiState,
                onVoteYes = viewModel::voteYes,
                onVoteNo = viewModel::voteNo,
                onOpenSearch = { navController.navigate("Search") },
                onOpenUpload = { navController.navigate("Upload") },
                onOpenProfile = { navController.navigate("Profile") },
                onOpenPost = { postId ->
                    viewModel.openPostDetail(postId)
                    navController.navigate("PostDetail")
                },
            )
        }
        composable("Search") {
            SearchScreen(
                state = viewModel.searchUiState,
                onBack = { navController.popBackStack() },
                onOpenPerson = { profileId ->
                    viewModel.openPersonProfile(profileId)
                    navController.navigate("Profile")
                },
                onOpenPost = { postId ->
                    viewModel.openPostDetail(postId)
                    navController.navigate("PostDetail")
                },
            )
        }
        composable("Upload") {
            UploadScreen(
                state = viewModel.uploadUiState,
                onBack = { navController.popBackStack() },
                onOccasionChange = viewModel::updateUploadOccasion,
                onPickPhotos = viewModel::requestUploadPhotoPicker,
                onSubmit = { viewModel.submitUpload(context.contentResolver) },
            )
        }
        composable("Profile") {
            ProfileScreen(
                state = viewModel.profileUiState,
                onBack = { navController.popBackStack() },
                onToggleFollow = viewModel::followSelectedProfile,
                onOpenFollowers = {
                    viewModel.openFollowers()
                    navController.navigate("FollowList")
                },
                onOpenFollowing = {
                    viewModel.openFollowing()
                    navController.navigate("FollowList")
                },
            )
        }
        composable("PostDetail") {
            PostDetailScreen(
                state = viewModel.postDetailUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable("FollowList") {
            FollowListScreen(
                state = viewModel.followListUiState,
                onBack = { navController.popBackStack() },
                onOpenPerson = { profileId ->
                    viewModel.openPersonProfile(profileId)
                    navController.navigate("Profile")
                },
            )
        }
    }
}
