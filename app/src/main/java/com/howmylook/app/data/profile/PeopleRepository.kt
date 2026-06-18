package com.howmylook.app.data.profile

import com.howmylook.app.data.post.onlyNonExpiredPosts
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.toFriendlyFollowError
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class PersonProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("total_liked_given") val totalLikedGiven: Int? = null,
    @SerialName("total_skipped_given") val totalSkippedGiven: Int? = null,
    @SerialName("total_picked_given") val totalPickedGiven: Int? = null,
)

@Serializable
private data class FollowRowDto(
    @SerialName("follower_id") val followerId: String? = null,
    @SerialName("following_id") val followingId: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
)

@Serializable
private data class PeopleVoteCountRowDto(
    @SerialName("post_id") val postId: String? = null,
)

@Serializable
private data class PeopleVisiblePostIdRowDto(
    @SerialName("id") val id: String? = null,
)

class PeopleRepository {
    private val profilePostRepository = ProfilePostRepository()

    suspend fun loadPersonProfile(
        config: SupabaseConfig,
        viewerUserId: String,
        profileId: String,
    ): Result<ProfileUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val profile = client.from("profiles")
                .select(columns = Columns.list("id", "username", "display_name", "bio", "avatar_url", "total_liked_given", "total_skipped_given", "total_picked_given")) {
                    filter { eq("id", profileId) }
                    limit(1)
                }
                .decodeSingleOrNull<PersonProfileDto>() ?: error("Profile not found.")

            val followers = client.from("follows")
                .select(columns = Columns.list("follower_id")) {
                    filter { eq("following_id", profileId) }
                    limit(200)
                }
                .decodeList<FollowRowDto>()

            val following = client.from("follows")
                .select(columns = Columns.list("following_id")) {
                    filter { eq("follower_id", profileId) }
                    limit(200)
                }
                .decodeList<FollowRowDto>()

            val viewerFollow = client.from("follows")
                .select(columns = Columns.list("follower_id", "following_id", "notifications_enabled")) {
                    filter {
                        eq("follower_id", viewerUserId)
                        eq("following_id", profileId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<FollowRowDto>()

            val yesVoteRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", profileId)
                        eq("value", "yes")
                    }
                }
                .decodeList<PeopleVoteCountRowDto>()

            val noVoteRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", profileId)
                        eq("value", "no")
                    }
                }
                .decodeList<PeopleVoteCountRowDto>()

            val posts = profilePostRepository.load(
                config = config,
                profileId = profileId,
                includePendingOwnPosts = false,
                viewerUserId = viewerUserId,
            ).getOrElse { emptyList() }
            val votedPostIds = (yesVoteRows.mapNotNull { it.postId } + noVoteRows.mapNotNull { it.postId }).distinct()
            val visibleVotedPostIds = if (votedPostIds.isEmpty()) {
                emptySet()
            } else {
                client.from("posts")
                    .select(columns = Columns.list("id")) {
                        filter {
                            isIn("id", votedPostIds)
                            eq("is_active", true)
                            eq("moderation_status", "approved")
                            onlyNonExpiredPosts()
                        }
                    }
                    .decodeList<PeopleVisiblePostIdRowDto>()
                    .mapNotNull { it.id }
                    .toSet()
            }
            val fallbackLikedCount = yesVoteRows.count { row -> row.postId != null && visibleVotedPostIds.contains(row.postId) }
            val fallbackSkippedCount = noVoteRows.count { row -> row.postId != null && visibleVotedPostIds.contains(row.postId) }

            ProfileUiState(
                loading = false,
                profileId = profile.id,
                displayName = profile.displayName ?: "HowMyLook user",
                username = profile.username?.let { "@$it" } ?: "@username",
                bio = profile.bio ?: "Posting looks and getting quick feedback.",
                avatarUrl = profile.avatarUrl,
                followers = followers.size,
                following = following.size,
                likedGiven = profile.totalLikedGiven ?: fallbackLikedCount,
                skippedGiven = profile.totalSkippedGiven ?: fallbackSkippedCount,
                pickedGiven = profile.totalPickedGiven ?: 0,
                posts = posts,
                isOwnProfile = viewerUserId == profileId,
                isFollowing = viewerFollow != null,
                notificationsEnabled = viewerFollow?.notificationsEnabled ?: false,
                error = null,
            )
        }
    }

    suspend fun setFollowing(
        config: SupabaseConfig,
        viewerUserId: String,
        profileId: String,
        shouldFollow: Boolean,
    ): Result<Unit> {
        return runCatching<Unit> {
            val client = SupabaseProvider.create(config)
            if (viewerUserId == profileId) {
                error("You can’t follow your own profile.")
            }
            if (shouldFollow) {
                client.from("follows").insert(
                    FollowRowDto(
                        followerId = viewerUserId,
                        followingId = profileId,
                    )
                )
            } else {
                client.from("follows").delete {
                    filter {
                        eq("follower_id", viewerUserId)
                        eq("following_id", profileId)
                    }
                }
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyFollowError(it.message), it)) },
        )
    }
}
