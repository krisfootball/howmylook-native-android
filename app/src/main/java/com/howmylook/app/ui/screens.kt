package com.howmylook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howmylook.app.data.auth.AuthFormState
import com.howmylook.app.data.auth.AuthMode
import com.howmylook.app.data.auth.SessionState
import com.howmylook.app.data.auth.UsernameFormState
import com.howmylook.app.data.feed.HomeDestination
import com.howmylook.app.data.feed.HomeUiState
import com.howmylook.app.data.feed.RatingCard
import com.howmylook.app.data.post.FollowListUiState
import com.howmylook.app.data.post.PostDetailUiState
import com.howmylook.app.data.profile.ProfileUiState
import com.howmylook.app.data.search.SearchUiState
import com.howmylook.app.data.upload.UploadUiState
import com.howmylook.app.domain.AppConfig

@Composable
fun SplashScreen() {
    CenteredLabel("Loading HowMyLook…")
}

@Composable
fun AuthScreen(
    state: AuthFormState,
    bootstrapMessage: String,
    onModeChange: (AuthMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(AppConfig.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Quick outfit feedback.")
        Text(
            text = bootstrapMessage,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onModeChange(AuthMode.SIGN_UP) },
                modifier = Modifier.weight(1f),
                enabled = !state.loading,
            ) {
                Text("Create account")
            }
            OutlinedButton(
                onClick = { onModeChange(AuthMode.SIGN_IN) },
                modifier = Modifier.weight(1f),
                enabled = !state.loading,
            ) {
                Text("Sign in")
            }
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            singleLine = true,
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
        )

        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), enabled = !state.loading) {
            Text(if (state.loading) "Please wait..." else if (state.mode == AuthMode.SIGN_UP) "Continue with email" else "Sign in")
        }

        if (state.message.isNotBlank()) {
            Text(state.message, modifier = Modifier.padding(top = 12.dp), color = Color(0xFF166534))
        }

        state.error?.let {
            Text(it, modifier = Modifier.padding(top = 12.dp), color = Color(0xFFB91C1C))
        }
    }
}

@Composable
fun UsernameScreen(
    state: UsernameFormState,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Choose your profile name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Pick the name people will see when they open your looks and profile.")

        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            singleLine = true,
        )

        OutlinedTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Display name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
        )

        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), enabled = !state.loading) {
            Text(if (state.loading) "Saving..." else "Save profile")
        }

        if (state.message.isNotBlank()) {
            Text(state.message, modifier = Modifier.padding(top = 12.dp), color = Color(0xFF166534))
        }

        state.error?.let {
            Text(it, modifier = Modifier.padding(top = 12.dp), color = Color(0xFFB91C1C))
        }
    }
}

@Composable
fun HomeScreen(
    sessionState: SessionState,
    card: RatingCard?,
    homeUiState: HomeUiState,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPost: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (homeUiState.destination == HomeDestination.UNLOCKED_HOME) "Home" else "Rate 5 looks to unlock everything",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (homeUiState.destination == HomeDestination.UNLOCKED_HOME) {
                "Unlocked. You can keep rating or move through the app."
            } else {
                "Progress: ${sessionState.unlockVotesCompleted}/${AppConfig.unlockVoteCount}"
            }
        )
        if (sessionState.availablePostCount in 0 until AppConfig.unlockVoteCount) {
            Text(
                "Only ${sessionState.availablePostCount} rateable post${if (sessionState.availablePostCount == 1) " is" else "s are"} available right now, so unlock may complete early.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }

        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp) {
            if (card == null) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No more looks are ready to rate right now.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(homeUiState.statusMessage)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!card.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = card.imageUrl,
                            contentDescription = card.occasion,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .background(Color(0xFFEAEAEA), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Photo placeholder", color = Color.DarkGray)
                        }
                    }

                    Button(onClick = { onOpenPost(card.id) }) { Text("Open") }
                    Text(card.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(card.occasion)
                    if (homeUiState.destination == HomeDestination.LOCKED_HOME) {
                        Text("Needs ${card.needsMoreRatings} more ratings")
                    }
                    Text("Yes ${card.yesCount} · No ${card.noCount}")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onVoteNo, modifier = Modifier.weight(1f)) { Text(AppConfig.noLabel) }
                        Button(onClick = onVoteYes, modifier = Modifier.weight(1f)) { Text(AppConfig.yesLabel) }
                    }
                }
            }
        }

        if (card != null && card.needsMoreRatings > 0 && homeUiState.destination == HomeDestination.LOCKED_HOME) {
            Text(
                "This look still needs ${card.needsMoreRatings} more ratings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }

        if (homeUiState.isLoading) {
            Text("Loading rating queue...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        if (homeUiState.statusMessage.isNotBlank()) {
            Text(homeUiState.statusMessage, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (sessionState.bootstrapMessage.isNotBlank() && sessionState.bootstrapMessage != homeUiState.statusMessage) {
            Text(sessionState.bootstrapMessage, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenSearch, modifier = Modifier.weight(1f)) { Text("Search") }
            Button(onClick = onOpenUpload, modifier = Modifier.weight(1f)) { Text("Post") }
            Button(onClick = onOpenProfile, modifier = Modifier.weight(1f)) { Text("Profile") }
        }
    }
}

@Composable
fun SearchScreen(state: SearchUiState, onBack: () -> Unit, onOpenPerson: (String) -> Unit, onOpenPost: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) { Text("Back") }
        Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Explore looks and people.", color = Color.Gray)

        if (state.loading) {
            Text("Loading search...", color = Color.Gray)
        }

        state.error?.let {
            Text(it, color = Color(0xFFB91C1C))
        }

        Text("Looks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.looks.forEach { look ->
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, onClick = { onOpenPost(look.id) }) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!look.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = look.imageUrl,
                            contentDescription = look.occasion,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(120.dp)
                                .weight(0.8f)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .weight(0.8f)
                                .background(Color(0xFFEAEAEA), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Preview", color = Color.DarkGray)
                        }
                    }
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(look.occasion, fontWeight = FontWeight.SemiBold)
                        Text("Yes ${look.yesCount} · No ${look.noCount}", color = Color.Gray)
                        if (look.imageCount > 1) {
                            Text("${look.imageCount} photos", color = Color.Gray)
                        }
                    }
                }
            }
        }

        Text("People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.people.forEach { person ->
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, onClick = { onOpenPerson(person.id) }) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(0.2f)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✨")
                    }
                    Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(person.displayName, fontWeight = FontWeight.SemiBold)
                        Text(person.username, color = Color.Gray)
                        Text(person.bio, color = Color.Gray)
                        Text(if (person.isFollowing) "Following" else "Open profile", color = Color(0xFFDB2777))
                    }
                }
            }
        }
    }
}

@Composable
fun UploadScreen(
    state: UploadUiState,
    onBack: () -> Unit,
    onOccasionChange: (String) -> Unit,
    onPickPhotos: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) { Text("Back") }
        Text("Post", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Choose 1 to 5 photos and add an occasion.", color = Color.Gray)

        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Upload outfit photos", fontWeight = FontWeight.SemiBold)
                Text("Start with 1 to 5 photos. New looks go to moderation first.", color = Color.Gray)
                Text("Selected photos: ${state.selectedPhotos.size}")
                if (state.selectedPhotoNames.isNotEmpty()) {
                    state.selectedPhotoNames.forEachIndexed { index, name ->
                        Text("• ${name.ifBlank { "Photo ${index + 1}" }}", color = Color.Gray)
                    }
                }
                Button(onClick = onPickPhotos, enabled = !state.loading) {
                    Text(if (state.selectedPhotos.isEmpty()) "Choose photos" else "Replace photos")
                }
            }
        }

        OutlinedTextField(
            value = state.occasion,
            onValueChange = onOccasionChange,
            label = { Text("Occasion") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading && state.selectedPhotos.isNotEmpty(),
        ) {
            Text(if (state.loading) "Publishing..." else "Publish look")
        }

        if (state.selectedPhotos.isEmpty()) {
            Text("Choose at least 1 photo before publishing.", color = Color.Gray)
        }

        if (state.message.isNotBlank()) {
            Text(state.message, color = Color(0xFF166534))
        }
        state.error?.let {
            Text(it, color = Color(0xFFB91C1C))
        }
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) { Text("Back") }

        if (state.loading) {
            Text("Loading profile...", color = Color.Gray)
        }

        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = state.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✨")
                    }
                }
                Text(state.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.username, color = Color.Gray)
                Text(state.bio)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenFollowers, enabled = !state.loading) { Text("Followers ${state.followers}") }
                    Button(onClick = onOpenFollowing, enabled = !state.loading) { Text("Following ${state.following}") }
                }
                Text("Yes given ${state.yesGiven} · No given ${state.noGiven}", color = Color.Gray)
                if (!state.isOwnProfile && state.profileId != null) {
                    Button(onClick = onToggleFollow, enabled = !state.loading) {
                        Text(if (state.loading) "Saving..." else if (state.isFollowing) "Following" else "Follow")
                    }
                }
            }
        }

        state.error?.let {
            Text(it, color = Color(0xFFB91C1C))
        }
    }
}

@Composable
fun PostDetailScreen(state: PostDetailUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) { Text("Back") }
        if (state.loading) {
            Text("Loading post...", color = Color.Gray)
        }
        state.error?.let { Text(it, color = Color(0xFFB91C1C)) }
        if (!state.loading && state.imageUrls.isEmpty() && state.error == null) {
            Text("No image available for this post.", color = Color.Gray)
        }
        if (state.imageUrls.isNotEmpty()) {
            AsyncImage(
                model = state.imageUrls.first(),
                contentDescription = state.occasion,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
        Text(state.authorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(state.occasion)
        Text("Yes ${state.yesCount} · No ${state.noCount}", color = Color.Gray)
    }
}

@Composable
fun FollowListScreen(state: FollowListUiState, onBack: () -> Unit, onOpenPerson: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) { Text("Back") }
        Text(state.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (state.loading) {
            Text("Loading ${state.title.lowercase()}...", color = Color.Gray)
        }
        state.error?.let { Text(it, color = Color(0xFFB91C1C)) }
        if (!state.loading && state.people.isEmpty()) {
            Text("No one here yet.", color = Color.Gray)
        }
        state.people.forEach { person ->
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, onClick = { onOpenPerson(person.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(person.displayName, fontWeight = FontWeight.SemiBold)
                    Text(person.username, color = Color.Gray)
                    Text(person.bio, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun SimpleScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun CenteredLabel(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
