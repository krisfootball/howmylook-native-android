package com.howmylook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howmylook.app.data.activity.ActivityUiState
import com.howmylook.app.data.admin.AdminUiState
import com.howmylook.app.data.auth.AuthFormState
import com.howmylook.app.data.auth.AuthMode
import com.howmylook.app.data.auth.SessionState
import com.howmylook.app.data.auth.UsernameFormState
import com.howmylook.app.data.feed.HomeDestination
import com.howmylook.app.data.feed.HomeUiState
import com.howmylook.app.data.feed.RatingCard
import com.howmylook.app.data.post.FollowListUiState
import com.howmylook.app.data.post.PostDetailUiState
import com.howmylook.app.data.profile.EditProfileFormState
import com.howmylook.app.data.profile.ProfileUiState
import com.howmylook.app.data.profile.VoteHistoryUiState
import com.howmylook.app.data.search.ExploreLookCard
import com.howmylook.app.data.search.SearchUiState
import com.howmylook.app.data.upload.UploadUiState
import com.howmylook.app.domain.AppConfig
import com.howmylook.app.domain.LegalDocument
import com.howmylook.app.domain.LegalDocumentType
import com.howmylook.app.domain.normalizeCompareSide
import com.howmylook.app.domain.resolveCompareVoteSide

private val PinkSurface = Color(0xFFFFF5FA)
private val SoftText = Color(0xFF64748B)
private val SuccessText = Color(0xFF166534)
private val ErrorText = Color(0xFFB91C1C)
private val AccentPink = Color(0xFFDB2777)
private val DarkButton = Color(0xFF020617)
private const val OCCASION_MAX_LENGTH = 80

private fun ExploreLookCard.isComparePost(): Boolean {
    return postKind == "compare" && !compareLeftImageUrl.isNullOrBlank() && !compareRightImageUrl.isNullOrBlank()
}

private fun comparePickPercents(leftCount: Int, rightCount: Int): Pair<Int, Int> {
    val total = leftCount + rightCount
    if (total == 0) return 50 to 50
    val leftPct = ((leftCount * 100.0) / total).toInt()
    return leftPct to (100 - leftPct)
}

private fun comparePickBadgeSide(
    selectedCompareSide: String?,
    compareLeftPickCount: Int,
    compareRightPickCount: Int,
): String? {
    if (selectedCompareSide == "left" || selectedCompareSide == "right") {
        return selectedCompareSide
    }
    normalizeCompareSide(selectedCompareSide)?.let { return it }
    return when {
        compareLeftPickCount > compareRightPickCount && compareLeftPickCount > 0 -> "left"
        compareRightPickCount > compareLeftPickCount && compareRightPickCount > 0 -> "right"
        else -> null
    }
}

private fun viewerCompareSide(selectedCompareSide: String?): String? {
    return resolveCompareVoteSide(selectedCompareSide, "compare")
}

private fun ExploreLookCard.viewerCompareSide(): String? {
    return viewerCompareSide(selectedCompareSide)
}

private fun ExploreLookCard.comparePickBadgeSide(): String? {
    return comparePickBadgeSide(selectedCompareSide, compareLeftPickCount, compareRightPickCount)
}

private fun ExploreLookCard.profileGridPickBadgeSide(isOwnProfile: Boolean): String? {
    viewerCompareSide()?.let { return it }
    return if (isOwnProfile) comparePickBadgeSide() else null
}

@Composable
private fun appTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    disabledTextColor = Color(0xFF475569),
    focusedLabelColor = SoftText,
    unfocusedLabelColor = SoftText,
    cursorColor = AccentPink,
)

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loading HowMyLook…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AuthScreen(
    state: AuthFormState,
    bootstrapMessage: String,
    debugMessage: String?,
    onModeChange: (AuthMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAcceptedPoliciesChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(AppConfig.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Quick outfit feedback.", color = SoftText)

        if (bootstrapMessage.isNotBlank()) {
            Text(
                text = bootstrapMessage,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = SoftText,
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFFFEEF6),
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthModeChip(
                    label = "Create account",
                    selected = state.mode == AuthMode.SIGN_UP,
                    onClick = { onModeChange(AuthMode.SIGN_UP) },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                )
                AuthModeChip(
                    label = "Sign in",
                    selected = state.mode == AuthMode.SIGN_IN,
                    onClick = { onModeChange(AuthMode.SIGN_IN) },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFFF6FA),
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.mode == AuthMode.SIGN_UP) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = state.acceptedPolicies,
                                onCheckedChange = onAcceptedPoliciesChange,
                                enabled = !state.loading,
                            )
                            SignupPolicyAgreementText(
                                modifier = Modifier.weight(1f),
                                onOpenLegalDocument = onOpenLegalDocument,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(16.dp),
                )

                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(16.dp),
                )

                if (state.mode == AuthMode.SIGN_IN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onForgotPassword,
                            enabled = !state.loading,
                        ) {
                            Text("Forgot password?", color = AccentPink)
                        }
                    }
                }

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(if (state.loading) "Please wait..." else if (state.mode == AuthMode.SIGN_UP) "Continue with email" else "Sign in")
                }
            }
        }

        if (state.message.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(22.dp),
                color = if (state.message.contains("Check your email")) Color(0xFFECFDF3) else Color.White,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (state.message.contains("Check your email")) "Check your email" else state.message,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.message.contains("Check your email")) SuccessText else MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.message.contains("Check your email")) {
                        Text(
                            "We sent you a confirmation link. Confirm your signup, then come back and sign in.",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessText.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        state.error?.let {
            Text(it, modifier = Modifier.padding(top = 12.dp), color = ErrorText)
        }

    }
}

@Composable
private fun SignupPolicyAgreementText(
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = SpanStyle(color = AccentPink, textDecoration = TextDecoration.Underline)
    val policyText = buildAnnotatedString {
        append("I agree to the ")
        pushStringAnnotation(tag = "legal", annotation = LegalDocumentType.Terms.name)
        withStyle(linkStyle) { append("Terms") }
        pop()
        append(", ")
        pushStringAnnotation(tag = "legal", annotation = LegalDocumentType.Privacy.name)
        withStyle(linkStyle) { append("Privacy Policy") }
        pop()
        append(", and ")
        pushStringAnnotation(tag = "legal", annotation = LegalDocumentType.Guidelines.name)
        withStyle(linkStyle) { append("Community Guidelines") }
        pop()
        append(".")
    }

    ClickableText(
        text = policyText,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(color = SoftText),
        onClick = { offset ->
            policyText.getStringAnnotations(tag = "legal", start = offset, end = offset)
                .firstOrNull()
                ?.let { runCatching { LegalDocumentType.valueOf(it.item) }.getOrNull() }
                ?.let(onOpenLegalDocument)
        },
    )
}

@Composable
private fun ReportContentDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tell us what is wrong. This is optional.", color = SoftText, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reason") },
                    minLines = 2,
                    colors = appTextFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().ifBlank { null }) }) {
                Text("Submit report", color = ErrorText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AuthModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) MaterialTheme.colorScheme.onSurface else SoftText, fontWeight = FontWeight.SemiBold)
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Choose your profile name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Pick the name people will see when they open your looks and profile.", color = SoftText)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = PinkSurface,
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(16.dp),
                )

                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(16.dp),
                )

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(if (state.loading) "Saving..." else "Save profile")
                }
            }
        }

        if (state.message.isNotBlank()) {
            Text(state.message, modifier = Modifier.padding(top = 12.dp), color = SuccessText)
        }

        state.error?.let {
            Text(it, modifier = Modifier.padding(top = 12.dp), color = ErrorText)
        }
    }
}

@Composable
fun HomeScreen(
    sessionState: SessionState,
    card: RatingCard?,
    homeUiState: HomeUiState,
    onOpenAuthorProfile: (String) -> Unit,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (card == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (homeUiState.destination == HomeDestination.LOCKED_HOME) {
                    LockBanner()
                }
                Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No more looks are ready to rate right now.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(homeUiState.statusMessage, color = SoftText)
                    }
                }
                if (homeUiState.isLoading) {
                    Text("Loading rating queue...", style = MaterialTheme.typography.bodySmall, color = SoftText)
                }
                if (sessionState.availablePostCount in 0 until AppConfig.unlockVoteCount) {
                    Text(
                        "Only ${sessionState.availablePostCount} rateable post${if (sessionState.availablePostCount == 1) " is" else "s are"} available right now, so unlock may complete early.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftText,
                    )
                }
            }
            return
        }

        if (card.postKind == "compare" && !card.compareLeftImageUrl.isNullOrBlank() && !card.compareRightImageUrl.isNullOrBlank()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(enabled = !homeUiState.isLoading) { onVoteNo() },
                ) {
                    AsyncImage(
                        model = card.compareLeftImageUrl,
                        contentDescription = "Left compare look",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (homeUiState.compareSelection == "left") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8038A169)),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.9f)),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(enabled = !homeUiState.isLoading) { onVoteYes() },
                ) {
                    AsyncImage(
                        model = card.compareRightImageUrl,
                        contentDescription = "Right compare look",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (homeUiState.compareSelection == "right") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8038A169)),
                        )
                    }
                }
            }
        } else if (!card.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = card.occasion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF6D6DF), Color(0xFFDFC8FF)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Photo placeholder", color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x66000000), Color.Transparent, Color(0xBF000000)),
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (homeUiState.destination == HomeDestination.LOCKED_HOME) {
                    LockBanner()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = card.authorName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAuthorProfile(card.authorId) }
                        .padding(vertical = 2.dp)
                )
                Text(card.occasion, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.titleMedium)
                if (card.postKind != "compare") {
                    Text(
                        "${card.yesCount} ${AppConfig.likedCountLabel} · ${card.noCount} ${AppConfig.skippedCountLabel}",
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
                if (sessionState.availablePostCount in 0 until AppConfig.unlockVoteCount) {
                    Text(
                        "Only ${sessionState.availablePostCount} rateable post${if (sessionState.availablePostCount == 1) " is" else "s are"} available now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                    )
                }
                if (card.postKind == "compare") {
                    Text(
                        "Tap a side or use the buttons below to pick your favorite.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComparePickButton(
                                selected = homeUiState.compareSelection == "left",
                                enabled = !homeUiState.isLoading,
                                onClick = onVoteNo,
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComparePickButton(
                                selected = homeUiState.compareSelection == "right",
                                enabled = !homeUiState.isLoading,
                                onClick = onVoteYes,
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onVoteNo,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text("✕ ${AppConfig.skipLabel}") }
                        Button(
                            onClick = onVoteYes,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text("✓ ${AppConfig.likeLabel}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparePickButton(selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color(0xFF22C55E) else Color.White.copy(alpha = 0.18f),
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Pick outfit",
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun LockBanner() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xCC000000)) {
            Text(
                "Rate 5 photos to access account",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SearchScreen(state: SearchUiState, onQueryChange: (String) -> Unit, onOpenPost: (String) -> Unit) {
    val query = state.query.trim().lowercase()
    val filteredLooks = if (query.isBlank()) state.looks else state.looks.filter {
        it.occasion.lowercase().contains(query) ||
            it.authorDisplayName.lowercase().contains(query) ||
            it.authorUsername.lowercase().contains(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = appTextFieldColors(),
                )
            }
        }

        if (state.loading) {
            Text("Loading...", color = SoftText)
        }
        state.error?.let {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF1F2)) {
                Text(it, modifier = Modifier.padding(16.dp), color = ErrorText)
            }
        }

        if (!state.loading && filteredLooks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredLooks.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { columnIndex, look ->
                            LookGridTile(
                                post = look,
                                rowIndex = rowIndex,
                                columnIndex = columnIndex,
                                modifier = Modifier.weight(1f),
                                showViewerPickBadge = false,
                                onClick = { onOpenPost(look.id) },
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminScreen(
    state: AdminUiState,
    onApprovePost: (String) -> Unit,
    onDeletePost: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading && state.posts.isEmpty()) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                Text("Loading posts...", modifier = Modifier.padding(16.dp), color = SoftText)
            }
        }

        state.error?.let {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF1F2)) {
                Text(it, modifier = Modifier.padding(16.dp), color = ErrorText)
            }
        }

        if (!state.loading && state.error == null && state.posts.isEmpty()) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                Text("No posts yet.", modifier = Modifier.padding(16.dp), color = SoftText)
            }
        }

        if (state.actionMessage.isNotBlank()) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFECFDF3)) {
                Text(state.actionMessage, modifier = Modifier.padding(16.dp), color = SuccessText)
            }
        }

        state.posts.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { columnIndex, post ->
                    AdminModerationTile(
                        post = post,
                        rowIndex = rowIndex,
                        columnIndex = columnIndex,
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f),
                        onApprove = { onApprovePost(post.id) },
                        onDelete = { onDeletePost(post.id) },
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdminModerationTile(
    post: ExploreLookCard,
    rowIndex: Int,
    columnIndex: Int,
    enabled: Boolean,
    onApprove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(2.dp)),
        ) {
            when {
                post.isComparePost() -> {
                    CompareSplitImages(
                        leftImageUrl = post.compareLeftImageUrl,
                        rightImageUrl = post.compareRightImageUrl,
                        leftContentDescription = "Compare left image",
                        rightContentDescription = "Compare right image",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                !post.imageUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = post.occasion,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    val colors = when ((rowIndex + columnIndex) % 3) {
                        0 -> listOf(Color(0xFFF6D6DF), Color(0xFFDFC8FF))
                        1 -> listOf(Color(0xFFF7E7C6), Color(0xFFEBB3B0))
                        else -> listOf(Color(0xFFC9D4FF), Color(0xFFDFB2F4))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
                }
            }
        }

        Text(
            text = post.authorDisplayName,
            style = MaterialTheme.typography.labelSmall,
            color = SoftText,
            maxLines = 1,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onApprove,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text("Approve", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text("Delete", style = MaterialTheme.typography.labelSmall, color = ErrorText)
            }
        }
    }
}

@Composable
fun UploadScreen(
    state: UploadUiState,
    onPostKindChange: (String) -> Unit,
    onOccasionChange: (String) -> Unit,
    onPickPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFF6FA), shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷")
                }
                Text("Upload outfit photos", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (state.postKind == "compare") "Choose exactly 2 photos for a side-by-side split post." else "Choose 1 to 5 photos, max 10 MB each, and add an occasion.", modifier = Modifier.padding(top = 6.dp), color = SoftText)

                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onPostKindChange("single") },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (state.postKind == "single") Color.White else DarkButton, containerColor = if (state.postKind == "single") DarkButton else Color.Transparent),
                    ) {
                        Text("Single")
                    }
                    OutlinedButton(
                        onClick = { onPostKindChange("compare") },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (state.postKind == "compare") Color.White else DarkButton, containerColor = if (state.postKind == "compare") DarkButton else Color.Transparent),
                    ) {
                        Text("Split")
                    }
                }

                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPickPhotos,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                    ) {
                        Text(if (state.selectedPhotos.isEmpty()) "Choose photos" else "Replace photos")
                    }
                    OutlinedButton(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkButton),
                    ) {
                        Text("Use camera")
                    }
                }
            }
        }

        if (state.selectedPhotoNames.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (state.postKind == "compare") "Selected compare photos: ${state.selectedPhotos.size}/2" else "Selected photos: ${state.selectedPhotos.size}", fontWeight = FontWeight.SemiBold)
                    if (state.postKind == "compare" && state.selectedPhotos.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        ) {
                            CompareSplitImages(
                                leftImageUrl = state.selectedPhotos[0],
                                rightImageUrl = state.selectedPhotos[1],
                                leftContentDescription = "Left compare photo",
                                rightContentDescription = "Right compare photo",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Text("Left and right will appear side by side in the feed.", color = SoftText, style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.selectedPhotoNames.forEachIndexed { index, name ->
                            Text("• ${name.ifBlank { "Photo ${index + 1}" }}", color = SoftText)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            colors = appTextFieldColors(),
            value = state.occasion,
            onValueChange = { onOccasionChange(it.take(OCCASION_MAX_LENGTH)) },
            label = { Text("Occasion") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            supportingText = { Text("${state.occasion.length}/$OCCASION_MAX_LENGTH") },
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading && ((state.postKind == "compare" && state.selectedPhotos.size == 2) || (state.postKind != "compare" && state.selectedPhotos.isNotEmpty())) && state.occasion.trim().isNotEmpty(),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
        ) {
            Text(if (state.loading) "Publishing..." else if (state.postKind == "compare") "Publish split" else "Publish look")
        }

        if (state.postKind == "compare") {
            if (state.selectedPhotos.size != 2) {
                Text("Choose exactly 2 photos before publishing a split post.", color = SoftText)
            }
        } else if (state.selectedPhotos.isEmpty()) {
            Text("Choose at least 1 photo before publishing.", color = SoftText)
        }
        if (state.occasion.trim().isEmpty()) {
            Text("Add an occasion before publishing.", color = SoftText)
        }
        if (state.message.isNotBlank()) {
            Text(state.message, color = SuccessText)
        }
        state.error?.let {
            Text(it, color = ErrorText)
        }
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleNotifications: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenYesGiven: () -> Unit,
    onOpenNoGiven: () -> Unit,
    onOpenPickedGiven: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPost: (String) -> Unit,
    onLogOut: () -> Unit,
    onReportProfile: (String?) -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val keptCount = state.posts.count { it.keepForever }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.loading) {
            Text("Loading profile...", color = SoftText)
        }

        Surface(
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFFFFBFD),
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!state.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.avatarUrl,
                            contentDescription = state.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFFF6C4D5), Color(0xFFDDB7FF))), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("✨") }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(state.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(state.username, color = SoftText)
                            }
                            if (state.isOwnProfile) {
                                Box {
                                    Text(
                                        "⋯",
                                        modifier = Modifier.clickable { menuExpanded = true }.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = AccentPink,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(text = { Text("Terms") }, onClick = {
                                            menuExpanded = false
                                            onOpenLegalDocument(LegalDocumentType.Terms)
                                        })
                                        DropdownMenuItem(text = { Text("Privacy") }, onClick = {
                                            menuExpanded = false
                                            onOpenLegalDocument(LegalDocumentType.Privacy)
                                        })
                                        DropdownMenuItem(text = { Text("Guidelines") }, onClick = {
                                            menuExpanded = false
                                            onOpenLegalDocument(LegalDocumentType.Guidelines)
                                        })
                                        DropdownMenuItem(text = { Text("Contact") }, onClick = {
                                            menuExpanded = false
                                            onOpenLegalDocument(LegalDocumentType.Contact)
                                        })
                                        DropdownMenuItem(text = { Text("Log out") }, onClick = {
                                            menuExpanded = false
                                            onLogOut()
                                        })
                                    }
                                }
                            }
                        }
                        Text(state.bio, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (state.isOwnProfile) {
                    Button(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                    ) {
                        Text("Edit profile")
                    }
                }

                if (!state.isOwnProfile && state.profileId != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onToggleFollow,
                            enabled = !state.loading,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isFollowing) Color.White else DarkButton,
                                contentColor = if (state.isFollowing) DarkButton else Color.White,
                            ),
                        ) {
                            Text(if (state.loading) "Saving..." else if (state.isFollowing) "Following" else "Follow")
                        }
                        if (state.isFollowing) {
                            Button(
                                onClick = onToggleNotifications,
                                enabled = !state.loading,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.notificationsEnabled) DarkButton else Color.White,
                                    contentColor = if (state.notificationsEnabled) Color.White else DarkButton,
                                ),
                            ) {
                                Text(if (state.notificationsEnabled) "Notifications on" else "Notify me")
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        enabled = !state.loading,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorText),
                    ) {
                        Text("Report profile")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { ProfileStatCard("FOLLOWERS", state.followers.toString(), onOpenFollowers) }
                        Box(modifier = Modifier.weight(1f)) { ProfileStatCard("FOLLOWING", state.following.toString(), onOpenFollowing) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { ProfileStatCard("LIKED", state.likedGiven.toString(), onOpenYesGiven) }
                        Box(modifier = Modifier.weight(1f)) { ProfileStatCard("SKIPPED", state.skippedGiven.toString(), onOpenNoGiven) }
                        Box(modifier = Modifier.weight(1f)) { ProfileStatCard("PICKED", state.pickedGiven.toString(), onOpenPickedGiven) }
                    }
                }
            }
        }

        if (state.isOwnProfile) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFFFDF3F8),
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Photos disappear after 30 days unless you keep them.", color = MaterialTheme.colorScheme.onSurface)
                    Text("$keptCount of 10 kept", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.posts.isEmpty()) {
            Surface(shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 1.dp) {
                Text("No published looks yet.", modifier = Modifier.padding(16.dp), color = SoftText)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.posts.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { columnIndex, post ->
                            LookGridTile(
                                post = post,
                                rowIndex = rowIndex,
                                columnIndex = columnIndex,
                                modifier = Modifier.weight(1f),
                                showKeepPin = state.isOwnProfile,
                                isOwnProfile = state.isOwnProfile,
                                onClick = { onOpenPost(post.id) },
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        state.error?.let {
            Text(it, color = ErrorText)
        }
        if (state.actionMessage.isNotBlank()) {
            Text(state.actionMessage, color = SuccessText)
        }
    }

    if (showReportDialog) {
        ReportContentDialog(
            title = "Report this profile?",
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                showReportDialog = false
                onReportProfile(reason)
            },
        )
    }
}

@Composable
private fun CompareSplitImages(
    leftImageUrl: String?,
    rightImageUrl: String?,
    leftContentDescription: String,
    rightContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        AsyncImage(
            model = leftImageUrl,
            contentDescription = leftContentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.9f)),
        )
        AsyncImage(
            model = rightImageUrl,
            contentDescription = rightContentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun LookGridTile(
    post: ExploreLookCard,
    rowIndex: Int,
    columnIndex: Int,
    modifier: Modifier = Modifier,
    showKeepPin: Boolean = false,
    showViewerPickBadge: Boolean = true,
    showLikedSkippedSummary: Boolean = true,
    isOwnProfile: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(180.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
    ) {
        when {
            post.isComparePost() -> {
                CompareSplitImages(
                    leftImageUrl = post.compareLeftImageUrl,
                    rightImageUrl = post.compareRightImageUrl,
                    leftContentDescription = "Compare left image",
                    rightContentDescription = "Compare right image",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            !post.imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.occasion,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                val colors = when ((rowIndex + columnIndex) % 3) {
                    0 -> listOf(Color(0xFFF6D6DF), Color(0xFFDFC8FF))
                    1 -> listOf(Color(0xFFF7E7C6), Color(0xFFEBB3B0))
                    else -> listOf(Color(0xFFC9D4FF), Color(0xFFDFB2F4))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
            }
        }

        if (showKeepPin && post.keepForever) {
            Text(
                "📌",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (post.isComparePost() && showViewerPickBadge) {
            CompareViewerPickOverlay(
                viewerSide = if (isOwnProfile) {
                    post.profileGridPickBadgeSide(isOwnProfile = true)
                } else {
                    post.viewerCompareSide()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (post.isComparePost() || showLikedSkippedSummary) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
                    .padding(8.dp),
            ) {
                if (post.isComparePost()) {
                    val (leftPct, rightPct) = comparePickPercents(post.compareLeftPickCount, post.compareRightPickCount)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "$leftPct%",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "$rightPct%",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${post.yesCount} ${AppConfig.likedCountLabel}",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Text(
                            text = "${post.noCount} ${AppConfig.skippedCountLabel}",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickedCheckBadge(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 12.dp,
    padding: androidx.compose.ui.unit.Dp = 5.dp,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.94f),
    ) {
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = AppConfig.pickedLabel,
                tint = Color.Black,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun CompareViewerPickOverlay(
    viewerSide: String?,
    modifier: Modifier = Modifier,
) {
    if (viewerSide == null) return

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(if (viewerSide == "left") Alignment.TopStart else Alignment.TopEnd)
                .fillMaxWidth(0.5f)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            PickedCheckBadge(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, onClick: (() -> Unit)?) {
    Surface(
        modifier = Modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = PinkSurface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.SemiBold)
            Text(label, color = SoftText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PostDetailScreen(
    state: PostDetailUiState,
    onOpenAuthorProfile: (() -> Unit)? = null,
    onToggleKeep: (() -> Unit)? = null,
    onDeletePost: (() -> Unit)? = null,
    onEditOccasion: ((String) -> Unit)? = null,
    onReportPost: ((String?) -> Unit)? = null,
    onVoteYes: (() -> Unit)? = null,
    onVoteNo: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var editExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var editOccasion by remember(state.occasion) { mutableStateOf(state.occasion) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    val isCompareDetail = state.postKind == "compare" &&
        !state.compareLeftImageUrl.isNullOrBlank() &&
        !state.compareRightImageUrl.isNullOrBlank()
    val viewerPickSide = viewerCompareSide(state.selectedCompareSide)
    val canRate = !state.isOwnPost && !state.hasViewerVoted && onVoteYes != null && onVoteNo != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (isCompareDetail) {
            Row(modifier = Modifier.fillMaxSize()) {
                CompareDetailPane(
                    imageUrl = state.compareLeftImageUrl,
                    contentDescription = "Left compare photo",
                    highlightedByViewer = viewerPickSide == "left",
                    showViewerPickBadge = viewerPickSide == "left",
                    hasViewerPick = viewerPickSide != null,
                    onImageClick = if (canRate) {
                        { onVoteNo?.invoke() }
                    } else {
                        { expandedImageUrl = state.compareLeftImageUrl }
                    },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.9f)),
                )
                CompareDetailPane(
                    imageUrl = state.compareRightImageUrl,
                    contentDescription = "Right compare photo",
                    highlightedByViewer = viewerPickSide == "right",
                    showViewerPickBadge = viewerPickSide == "right",
                    hasViewerPick = viewerPickSide != null,
                    onImageClick = if (canRate) {
                        { onVoteYes?.invoke() }
                    } else {
                        { expandedImageUrl = state.compareRightImageUrl }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (state.imageUrls.isNotEmpty()) {
            AsyncImage(
                model = state.imageUrls.first(),
                contentDescription = state.occasion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!isCompareDetail) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x24000000), Color.Transparent, Color(0xC4000000)),
                        )
                    )
            )
        }

        if (!state.isOwnPost && onReportPost != null) {
            TextButton(
                onClick = { showReportDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 16.dp)
                    .zIndex(3f),
            ) {
                Text("Report", color = Color.White)
            }
        }

        if (isCompareDetail && state.isOwnPost && onToggleKeep != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(start = 16.dp, end = 16.dp, top = 36.dp)
                    .zIndex(2f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Button(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x8C4B4B4B), contentColor = Color.White),
                    ) {
                        Text("⋯", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (state.keepForever) "Unkeep photo" else "Keep photo") },
                            onClick = {
                                menuExpanded = false
                                onToggleKeep()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit photo", color = SoftText) },
                            onClick = {
                                menuExpanded = false
                                editOccasion = state.occasion
                                editExpanded = true
                            },
                            enabled = onEditOccasion != null,
                        )
                        DropdownMenuItem(
                            text = { Text("Delete photo", color = ErrorText) },
                            onClick = {
                                menuExpanded = false
                                onDeletePost?.invoke()
                            },
                            enabled = onDeletePost != null,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .then(
                    if (isCompareDetail) {
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000)),
                                )
                            )
                            .zIndex(2f)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .padding(start = 16.dp, end = 16.dp, top = if (isCompareDetail) 0.dp else 36.dp, bottom = 18.dp),
            verticalArrangement = if (isCompareDetail) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween,
        ) {
            if (!isCompareDetail) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isOwnPost && onToggleKeep != null) {
                        Box {
                            Button(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x8C4B4B4B), contentColor = Color.White),
                            ) {
                                Text("⋯", fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (state.keepForever) "Unkeep photo" else "Keep photo") },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleKeep()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit photo", color = SoftText) },
                                    onClick = {
                                        menuExpanded = false
                                        editOccasion = state.occasion
                                        editExpanded = true
                                    },
                                    enabled = onEditOccasion != null,
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete photo", color = ErrorText) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePost?.invoke()
                                    },
                                    enabled = onDeletePost != null,
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editExpanded) {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.96f), shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Edit occasion", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = editOccasion,
                                onValueChange = { editOccasion = it.take(OCCASION_MAX_LENGTH) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Occasion") },
                                singleLine = true,
                                supportingText = { Text("${editOccasion.length}/$OCCASION_MAX_LENGTH") },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        editExpanded = false
                                        onEditOccasion?.invoke(editOccasion)
                                    },
                                    enabled = !state.loading,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                                ) {
                                    Text("Save")
                                }
                                Button(
                                    onClick = { editExpanded = false },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkSurface, contentColor = Color(0xFF0F172A)),
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
                Text(
                    text = state.authorName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = onOpenAuthorProfile != null) { onOpenAuthorProfile?.invoke() }
                        .padding(vertical = 2.dp)
                )
                Text(state.occasion, color = Color.White, style = MaterialTheme.typography.titleLarge)
                if (state.postKind == "compare") {
                    val (leftPct, rightPct) = comparePickPercents(state.compareLeftPickCount, state.compareRightPickCount)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "$leftPct%",
                            color = Color.White.copy(alpha = 0.84f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "$rightPct%",
                            color = Color.White.copy(alpha = 0.84f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Text(
                        "${state.yesCount} ${AppConfig.likedCountLabel}    ${state.noCount} ${AppConfig.skippedCountLabel}",
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (canRate) {
                    if (state.postKind == "compare") {
                        Text(
                            "Tap a side or use the buttons below to pick your favorite.",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                ComparePickButton(
                                    selected = false,
                                    enabled = !state.loading,
                                    onClick = { onVoteNo?.invoke() },
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                ComparePickButton(
                                    selected = false,
                                    enabled = !state.loading,
                                    onClick = { onVoteYes?.invoke() },
                                )
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onVoteNo?.invoke() },
                                enabled = !state.loading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White),
                                shape = RoundedCornerShape(999.dp),
                            ) { Text("✕ ${AppConfig.skipLabel}") }
                            Button(
                                onClick = { onVoteYes?.invoke() },
                                enabled = !state.loading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(999.dp),
                            ) { Text("✓ ${AppConfig.likeLabel}") }
                        }
                    }
                }
                if (state.actionMessage.isNotBlank()) {
                    Text(state.actionMessage, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
                state.error?.let {
                    Text(it, color = Color(0xFFFDA4AF), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        expandedImageUrl?.let { imageUrl ->
            val showPickedBadge = when (imageUrl) {
                state.compareLeftImageUrl -> viewerPickSide == "left"
                state.compareRightImageUrl -> viewerPickSide == "right"
                else -> false
            }
            FullScreenImageViewer(
                imageUrl = imageUrl,
                showPickedBadge = showPickedBadge,
                onDismiss = { expandedImageUrl = null },
            )
        }
    }

    if (showReportDialog && onReportPost != null) {
        ReportContentDialog(
            title = "Report this post?",
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                showReportDialog = false
                onReportPost(reason)
            },
        )
    }
}

@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    showPickedBadge: Boolean = false,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(3f)
            .background(Color.Black)
            .clickable(onClick = onDismiss),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Full photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (showPickedBadge) {
            PickedCheckBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                iconSize = 16.dp,
                padding = 8.dp,
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable(onClick = onDismiss),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
        ) {
            Text(
                "✕",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CompareDetailPane(
    imageUrl: String?,
    contentDescription: String,
    highlightedByViewer: Boolean,
    showViewerPickBadge: Boolean,
    hasViewerPick: Boolean,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onImageClick),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (hasViewerPick && !highlightedByViewer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
            )
        }
        if (showViewerPickBadge) {
            PickedCheckBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                iconSize = 16.dp,
                padding = 8.dp,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.9f)),
            )
        }
    }
}

@Composable
fun FollowListScreen(state: FollowListUiState, onBack: () -> Unit, onOpenPerson: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading) {
            Text("Loading ${state.title.lowercase()}...", color = SoftText)
        }
        state.error?.let { Text(it, color = ErrorText) }
        if (!state.loading && state.people.isEmpty()) {
            Text("No one here yet.", color = SoftText)
        }
        state.people.forEach { person ->
            Surface(
                modifier = Modifier.clickable { onOpenPerson(person.id) },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!person.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = person.avatarUrl,
                            contentDescription = person.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFFF6C4D5), Color(0xFFDDB7FF))), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("✨") }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(person.displayName, fontWeight = FontWeight.SemiBold)
                        Text(person.username, color = SoftText)
                    }
                }
            }
        }
    }
}

@Composable
fun VoteHistoryScreen(state: VoteHistoryUiState, onBack: () -> Unit, onOpenPost: (ExploreLookCard) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading) {
            Text("Loading ${state.title.lowercase()}...", color = SoftText)
        }
        state.error?.let { Text(it, color = ErrorText) }
        if (!state.loading && state.posts.isEmpty()) {
            Text("No posts here yet.", color = SoftText)
        }
        if (!state.loading && state.posts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.posts.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { columnIndex, post ->
                            LookGridTile(
                                post = post,
                                rowIndex = rowIndex,
                                columnIndex = columnIndex,
                                modifier = Modifier.weight(1f),
                                showLikedSkippedSummary = state.title != AppConfig.pickedLabel,
                                onClick = { onOpenPost(post) },
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    state: EditProfileFormState,
    onBack: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onKeepCurrentPhoto: () -> Unit,
    onSave: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (state.removeAvatar) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFFF6C4D5), Color(0xFFDDB7FF))), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("✨") }
                    } else if (!state.selectedAvatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = state.selectedAvatarUri,
                            contentDescription = "Selected profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape),
                        )
                    } else if (!state.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.avatarUrl,
                            contentDescription = "Current profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFFF6C4D5), Color(0xFFDDB7FF))), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("✨") }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Profile photo", fontWeight = FontWeight.SemiBold)
                        Text("Upload a square photo if you can. Max 5 MB. This is optional.", color = SoftText, style = MaterialTheme.typography.bodySmall)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onPickPhoto,
                                enabled = !state.loading && !state.saving,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkSurface, contentColor = Color(0xFF0F172A)),
                            ) {
                                Text(
                                    when {
                                        !state.selectedAvatarUri.isNullOrBlank() -> "Change selected photo"
                                        !state.avatarUrl.isNullOrBlank() && !state.removeAvatar -> "Change photo"
                                        else -> "Choose photo"
                                    }
                                )
                            }
                            if ((!state.avatarUrl.isNullOrBlank() || !state.selectedAvatarUri.isNullOrBlank()) && !state.removeAvatar) {
                                Button(
                                    onClick = onRemovePhoto,
                                    enabled = !state.loading && !state.saving,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurface),
                                ) {
                                    Text("Remove photo")
                                }
                            }
                            if (state.removeAvatar) {
                                Button(
                                    onClick = onKeepCurrentPhoto,
                                    enabled = !state.loading && !state.saving,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurface),
                                ) {
                                    Text("Keep current photo")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading && !state.saving,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.loading && !state.saving,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    colors = appTextFieldColors(),
                    value = state.bio,
                    onValueChange = onBioChange,
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    enabled = !state.loading && !state.saving,
                    shape = RoundedCornerShape(16.dp),
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && !state.saving && !state.deleting,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkButton, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(if (state.saving) "Saving..." else "Save profile")
                }
                if (state.message.isNotBlank()) {
                    Text(state.message, color = SuccessText)
                }
                state.error?.let { Text(it, color = ErrorText) }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Delete account",
                    color = ErrorText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Permanently delete your profile, photos, votes, follows, and login. This cannot be undone.",
                    color = SoftText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && !state.saving && !state.deleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F2), contentColor = ErrorText),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(if (state.deleting) "Deleting account..." else "Delete account forever")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.deleting) {
                    showDeleteDialog = false
                }
            },
            title = { Text("Delete account forever?") },
            text = {
                Text(
                    "This permanently removes your profile, outfit photos, votes, follows, notifications, and sign-in access.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    enabled = !state.deleting,
                ) {
                    Text("Delete forever", color = ErrorText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !state.deleting,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileStatCard(label: String, value: String, onClick: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF9EEF4),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(label, color = AccentPink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AccentPink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    Button(
        onClick = onBack,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text("Back")
    }
}

@Composable
fun ActivityScreen(state: ActivityUiState, onOpenProfile: (String) -> Unit, onOpenPost: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                Text("Loading activity...", modifier = Modifier.padding(16.dp), color = SoftText)
            }
        }
        state.error?.let {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF1F2), shadowElevation = 1.dp) {
                Text(it, modifier = Modifier.padding(16.dp), color = ErrorText)
            }
        }
        if (!state.loading && state.error == null && state.items.isEmpty()) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                Text("No activity yet.", modifier = Modifier.padding(16.dp), color = SoftText)
            }
        }
        state.items.forEach { item ->
            Surface(
                modifier = Modifier.clickable(enabled = item.targetProfileId != null || item.targetPostId != null) {
                    when {
                        item.targetProfileId != null -> onOpenProfile(item.targetProfileId)
                        item.targetPostId != null -> onOpenPost(item.targetPostId)
                    }
                },
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold)
                    if (item.subtitle.isNotBlank()) {
                        Text(item.subtitle, color = SoftText)
                    }
                }
            }
        }
    }
}

@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF6FB), Color(0xFFF5EDF8)),
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = AccentPink, fontWeight = FontWeight.SemiBold)
            }
        }

        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(document.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Last updated: ${document.lastUpdated}", color = SoftText, style = MaterialTheme.typography.bodySmall)

                document.intro?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                document.sections.forEach { section ->
                    Text(section.heading, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    section.paragraphs.forEach { paragraph ->
                        if (document.type == LegalDocumentType.Contact && paragraph.startsWith("Email:")) {
                            Row {
                                Text("Email: ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = AppConfig.supportEmail,
                                    color = AccentPink,
                                    textDecoration = TextDecoration.Underline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri("mailto:${AppConfig.supportEmail}")
                                    },
                                )
                            }
                        } else {
                            Text(paragraph, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
