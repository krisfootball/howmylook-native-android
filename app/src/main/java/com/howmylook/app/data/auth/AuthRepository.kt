package com.howmylook.app.data.auth

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.domain.AppConfig
import com.howmylook.app.domain.AppStep
import com.howmylook.app.domain.getNextRequiredStep
import com.howmylook.app.domain.hasCompletedUsername
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class AuthRepository {
    fun resolveStep(
        isAuthenticated: Boolean,
        profile: ProfileRecord?,
        availablePostCount: Int,
    ): AppStep {
        return getNextRequiredStep(
            isAuthenticated = isAuthenticated,
            hasUsername = hasCompletedUsername(profile?.id, profile?.username),
            ratingsCompleted = profile?.loginRatingVotesCompleted ?: 0,
            unlockVoteCount = AppConfig.unlockVoteCount,
            bypassRatingGate = availablePostCount < AppConfig.unlockVoteCount,
        )
    }

    suspend fun signUp(config: SupabaseConfig, email: String, password: String): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            if (result?.id == null) {
                "Check your email. Confirm signup, then come back and sign in."
            } else {
                "Account created."
            }
        }
    }

    suspend fun signIn(config: SupabaseConfig, email: String, password: String): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    suspend fun loadCurrentProfile(config: SupabaseConfig): Result<ProfileRecord?> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val user = client.auth.retrieveUserForCurrentSession(updateSession = true)
            val existing = client.from("profiles")
                .select(columns = Columns.list("id", "username", "display_name", "login_rating_votes_completed")) {
                    filter { eq("id", user.id) }
                    limit(1)
                }
                .decodeSingleOrNull<ProfileDto>()
                ?.toRecord()

            existing ?: ProfileRecord(
                id = user.id,
                username = null,
                displayName = null,
                loginRatingVotesCompleted = 0,
            )
        }
    }

    suspend fun saveUsername(
        config: SupabaseConfig,
        userId: String,
        username: String,
        displayName: String,
    ): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val cleanUsername = username.trim().lowercase()

            require(cleanUsername.length >= 3) { "Username must be at least 3 characters." }
            require(cleanUsername.matches(Regex("^[a-z0-9_]+$"))) { "Username can only use lowercase letters, numbers, and underscores." }

            val existing = client.from("profiles")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("username", cleanUsername)
                        neq("id", userId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<PostIdRowDto>()

            if (existing != null) {
                error("That username is already taken.")
            }

            client.from("profiles").upsert(
                ProfileUpsertDto(
                    id = userId,
                    username = cleanUsername,
                    displayName = displayName.trim().ifBlank { null },
                )
            )
        }
    }

    suspend fun resetLoginRatingCounter(config: SupabaseConfig, userId: String): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("profiles").update({
                set("login_rating_votes_completed", 0)
            }) {
                filter { eq("id", userId) }
            }
        }
    }

    suspend fun getAvailableRatingPostCount(config: SupabaseConfig, userId: String): Result<Int> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val voteRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<VotePostIdRowDto>()

            val posts = client.from("posts")
                .select(columns = Columns.list("id")) {
                    filter {
                        neq("user_id", userId)
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                    }
                    limit(100)
                }
                .decodeList<AvailablePostDto>()

            val ratedPostIds = voteRows.map { it.postId }.toSet()
            posts.count { post -> post.id !in ratedPostIds }
        }
    }
}
