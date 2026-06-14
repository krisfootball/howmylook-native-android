package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileDetailsDto(
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("total_liked_given") val totalLikedGiven: Int? = null,
    @SerialName("total_skipped_given") val totalSkippedGiven: Int? = null,
    @SerialName("total_picked_given") val totalPickedGiven: Int? = null,
)

@Serializable
private data class ProfileVoteCountRowDto(
    @SerialName("post_id") val postId: String? = null,
)

@Serializable
private data class VisiblePostIdRowDto(
    @SerialName("id") val id: String? = null,
)

class ProfileRepository {
    private val profilePostRepository = ProfilePostRepository()

    suspend fun loadOwnProfile(config: SupabaseConfig, userId: String): Result<ProfileUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val profile = client.from("profiles")
                .select(columns = Columns.list("username", "display_name", "bio", "avatar_url", "total_liked_given", "total_skipped_given", "total_picked_given")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<ProfileDetailsDto>()

            val followersCount = client.from("follows")
                .select(columns = Columns.list("follower_id")) {
                    filter { eq("following_id", userId) }
                }
                .decodeList<Map<String, String?>>()
                .size

            val followingCount = client.from("follows")
                .select(columns = Columns.list("following_id")) {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<Map<String, String?>>()
                .size

            val yesVoteRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "yes")
                    }
                }
                .decodeList<ProfileVoteCountRowDto>()

            val noVoteRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "no")
                    }
                }
                .decodeList<ProfileVoteCountRowDto>()

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
                        }
                    }
                    .decodeList<VisiblePostIdRowDto>()
                    .mapNotNull { it.id }
                    .toSet()
            }
            val fallbackLikedCount = yesVoteRows.count { row -> row.postId != null && visibleVotedPostIds.contains(row.postId) }
            val fallbackSkippedCount = noVoteRows.count { row -> row.postId != null && visibleVotedPostIds.contains(row.postId) }

            val posts = profilePostRepository.load(config, userId, includePendingOwnPosts = true).getOrElse { emptyList() }

            ProfileUiState(
                loading = false,
                profileId = userId,
                displayName = profile?.displayName ?: "Your profile",
                username = profile?.username?.let { "@$it" } ?: "@username",
                bio = profile?.bio ?: "No bio yet.",
                avatarUrl = profile?.avatarUrl,
                likedGiven = profile?.totalLikedGiven ?: fallbackLikedCount,
                skippedGiven = profile?.totalSkippedGiven ?: fallbackSkippedCount,
                pickedGiven = profile?.totalPickedGiven ?: 0,
                followers = followersCount,
                following = followingCount,
                posts = posts,
                isOwnProfile = true,
                isFollowing = false,
                error = null,
            )
        }
    }
}
