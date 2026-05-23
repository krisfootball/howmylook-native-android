package com.howmylook.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.howmylook.app.domain.AppRoute
import com.howmylook.app.ui.screens.ActivityScreen
import com.howmylook.app.ui.screens.AuthScreen
import com.howmylook.app.ui.screens.EditProfileScreen
import com.howmylook.app.ui.screens.FollowListScreen
import com.howmylook.app.ui.screens.HomeScreen
import com.howmylook.app.ui.screens.PostDetailScreen
import com.howmylook.app.ui.screens.ProfileScreen
import com.howmylook.app.ui.screens.SearchScreen
import com.howmylook.app.ui.screens.SplashScreen
import com.howmylook.app.ui.screens.UploadScreen
import com.howmylook.app.ui.screens.UsernameScreen
import com.howmylook.app.ui.screens.VoteHistoryScreen
import com.howmylook.app.viewmodel.AppViewModel

data class BottomNavItem(
    val route: AppRoute,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val startDestination = viewModel.resolveStartRoute().name
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(AppRoute.Home, "Home") { Icon(Icons.Outlined.Home, contentDescription = "Home") },
        BottomNavItem(AppRoute.Search, "Search") { Icon(Icons.Outlined.Search, contentDescription = "Search") },
        BottomNavItem(AppRoute.Upload, "Post") { Icon(Icons.Outlined.AddBox, contentDescription = "Post") },
        BottomNavItem(AppRoute.Activity, "Activity") { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Activity") },
        BottomNavItem(AppRoute.Profile, "Profile") { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
    )
    val availableBottomNavItems = if (viewModel.sessionState.needsUnlockRatings) {
        bottomNavItems.filter { it.route == AppRoute.Home }
    } else {
        bottomNavItems
    }

    val bottomBarRoutes = availableBottomNavItems.map { it.route.name } + listOf(
        AppRoute.PostDetail.name,
        AppRoute.FollowList.name,
        AppRoute.VoteHistory.name,
        AppRoute.EditProfile.name,
    )
    val profileBackstackRoutes = setOf(
        AppRoute.PostDetail.name,
        AppRoute.FollowList.name,
        AppRoute.VoteHistory.name,
        AppRoute.EditProfile.name,
        AppRoute.Profile.name,
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            viewModel.setSelectedUploadPhotos(uris.map { it.toString() })
        },
    )
    val editAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.setEditAvatar(uri?.toString())
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
        viewModel.sessionState.needsUnlockRatings,
    ) {
        val routeName = viewModel.resolveStartRoute().name
        if (navController.currentDestination?.route != routeName) {
            navController.navigate(routeName) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFFFF6FB),
        bottomBar = {
            if (showBottomBar) {
                Surface(shadowElevation = 10.dp) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                    ) {
                        availableBottomNavItems.forEach { item ->
                            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.route.name } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (viewModel.sessionState.needsUnlockRatings && item.route != AppRoute.Home) {
                                        navController.navigate(AppRoute.Home.name) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else if (item.route == AppRoute.Profile) {
                                        if (currentRoute != AppRoute.Profile.name) {
                                            viewModel.openOwnProfile()
                                        }
                                        val currentBackStackRoute = currentRoute
                                        if (currentBackStackRoute in profileBackstackRoutes) {
                                            val popped = navController.popBackStack(AppRoute.Profile.name, inclusive = false)
                                            if (!popped) {
                                                navController.navigate(AppRoute.Profile.name) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(AppRoute.Profile.name) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(item.route.name) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = item.icon,
                                label = {
                                    Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFDB2777),
                                    selectedTextColor = Color(0xFFDB2777),
                                    indicatorColor = Color(0xFFFFEEF6),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B),
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp)),
        ) {
            composable(AppRoute.Splash.name) { SplashScreen() }
            composable(AppRoute.Auth.name) {
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
            composable(AppRoute.Username.name) {
                UsernameScreen(
                    state = viewModel.usernameFormState,
                    onUsernameChange = viewModel::updateUsername,
                    onDisplayNameChange = viewModel::updateDisplayName,
                    onSubmit = viewModel::submitUsername,
                )
            }
            composable(AppRoute.Home.name) {
                HomeScreen(
                    sessionState = viewModel.sessionState,
                    card = viewModel.currentCard,
                    homeUiState = viewModel.homeUiState,
                    onVoteYes = viewModel::voteYes,
                    onVoteNo = viewModel::voteNo,
                )
            }
            composable(AppRoute.Search.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    SearchScreen(
                        state = viewModel.searchUiState,
                        onOpenPost = { postId ->
                            viewModel.openPostDetail(postId, fromRoute = AppRoute.Search.name)
                            navController.navigate(AppRoute.PostDetail.name)
                        },
                    )
                }
            }
            composable(AppRoute.Upload.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    UploadScreen(
                        state = viewModel.uploadUiState,
                        onOccasionChange = viewModel::updateUploadOccasion,
                        onPickPhotos = viewModel::requestUploadPhotoPicker,
                        onSubmit = { viewModel.submitUpload(context.contentResolver) },
                    )
                }
            }
            composable(AppRoute.Profile.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    ProfileScreen(
                        state = viewModel.profileUiState,
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(AppRoute.Home.name) { launchSingleTop = true }
                            }
                        },
                        onToggleFollow = viewModel::followSelectedProfile,
                        onToggleNotifications = viewModel::toggleProfileNotifications,
                        onOpenFollowers = {
                            viewModel.openFollowers()
                            navController.navigate(AppRoute.FollowList.name)
                        },
                        onOpenFollowing = {
                            viewModel.openFollowing()
                            navController.navigate(AppRoute.FollowList.name)
                        },
                        onOpenYesGiven = {
                            viewModel.openYesGiven()
                            navController.navigate(AppRoute.VoteHistory.name)
                        },
                        onOpenNoGiven = {
                            viewModel.openNoGiven()
                            navController.navigate(AppRoute.VoteHistory.name)
                        },
                        onEditProfile = {
                            viewModel.startEditProfile()
                            navController.navigate(AppRoute.EditProfile.name)
                        },
                        onOpenPost = { postId ->
                            viewModel.openPostDetail(postId, fromRoute = AppRoute.Profile.name)
                            navController.navigate(AppRoute.PostDetail.name)
                        },
                        onLogOut = {
                            viewModel.signOut()
                            navController.navigate(AppRoute.Auth.name) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            composable(AppRoute.Activity.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    ActivityScreen(
                        state = viewModel.activityUiState,
                        onOpenProfile = { profileId ->
                            viewModel.openPersonProfile(profileId)
                            navController.navigate(AppRoute.Profile.name)
                        },
                        onOpenPost = { postId ->
                            viewModel.openPostDetail(postId, fromRoute = AppRoute.Activity.name)
                            navController.navigate(AppRoute.PostDetail.name)
                        },
                    )
                }
            }
            composable(AppRoute.PostDetail.name) {
                PostDetailScreen(
                    state = viewModel.postDetailUiState,
                    onBack = {
                        val backRoute = when (viewModel.postDetailUiState.fromRoute) {
                            AppRoute.Search.name -> AppRoute.Search.name
                            AppRoute.Profile.name -> AppRoute.Profile.name
                            AppRoute.VoteHistory.name -> AppRoute.VoteHistory.name
                            AppRoute.Activity.name -> AppRoute.Activity.name
                            else -> null
                        }
                        if (backRoute != null) {
                            navController.navigate(backRoute) {
                                popUpTo(AppRoute.PostDetail.name) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onToggleKeep = viewModel::toggleKeepCurrentPost,
                    onDeletePost = viewModel::deleteCurrentPost,
                    onEditOccasion = viewModel::editCurrentPostOccasion,
                )
            }
            composable(AppRoute.FollowList.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    FollowListScreen(
                        state = viewModel.followListUiState,
                        onBack = { navController.popBackStack() },
                        onOpenPerson = { profileId ->
                            if (!viewModel.sessionState.needsUnlockRatings) {
                                viewModel.openPersonProfile(profileId)
                                navController.navigate(AppRoute.Profile.name)
                            }
                        },
                    )
                }
            }
            composable(AppRoute.VoteHistory.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    VoteHistoryScreen(
                        state = viewModel.voteHistoryUiState,
                        onBack = { navController.popBackStack() },
                        onOpenPost = { postId ->
                            viewModel.openPostDetail(postId, fromRoute = AppRoute.VoteHistory.name)
                            navController.navigate(AppRoute.PostDetail.name)
                        },
                    )
                }
            }
            composable(AppRoute.EditProfile.name) {
                if (viewModel.sessionState.needsUnlockRatings) {
                    LaunchedEffect(Unit) { navController.navigate(AppRoute.Home.name) { launchSingleTop = true } }
                } else {
                    EditProfileScreen(
                        state = viewModel.editProfileFormState,
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(AppRoute.Profile.name) { launchSingleTop = true }
                            }
                        },
                        onUsernameChange = viewModel::updateEditUsername,
                        onDisplayNameChange = viewModel::updateEditDisplayName,
                        onBioChange = viewModel::updateEditBio,
                        onPickPhoto = {
                        editAvatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = viewModel::markEditAvatarForRemoval,
                    onKeepCurrentPhoto = viewModel::cancelEditAvatarRemoval,
                    onSave = { viewModel.submitEditProfile(context.contentResolver) },
                    )
                }
            }
        }
    }
}
