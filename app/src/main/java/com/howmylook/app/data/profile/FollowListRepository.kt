package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.post.FollowListPerson
import com.howmylook.app.data.post.FollowListUiState
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FollowEdgeDto(
    @SerialName("follower_id") val followerId: String? = null,
    @SerialName("following_id") val followingId: String? = null,
)

@Serializable
private data class FollowProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

class FollowListRepository {
    suspend fun loadFollowers(config: SupabaseConfig, profileId: String): Result<FollowListUiState> {
        return load(config, profileId, mode = "followers")
    }

    suspend fun loadFollowing(config: SupabaseConfig, profileId: String): Result<FollowListUiState> {
        return load(config, profileId, mode = "following")
    }

    private suspend fun load(config: SupabaseConfig, profileId: String, mode: String): Result<FollowListUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val edges = if (mode == "followers") {
                client.from("follows")
                    .select(columns = Columns.list("follower_id")) {
                        filter { eq("following_id", profileId) }
                        limit(200)
                    }
                    .decodeList<FollowEdgeDto>()
                    .mapNotNull { it.followerId }
            } else {
                client.from("follows")
                    .select(columns = Columns.list("following_id")) {
                        filter { eq("follower_id", profileId) }
                        limit(200)
                    }
                    .decodeList<FollowEdgeDto>()
                    .mapNotNull { it.followingId }
            }

            val people = edges.mapNotNull { id ->
                client.from("profiles")
                    .select(columns = Columns.list("id", "display_name", "username", "avatar_url")) {
                        filter { eq("id", id) }
                        limit(1)
                    }
                    .decodeSingleOrNull<FollowProfileDto>()
            }.map {
                FollowListPerson(
                    id = it.id,
                    displayName = it.displayName ?: "HowMyLook user",
                    username = it.username?.let { username -> "@$username" } ?: "@username",
                    avatarUrl = it.avatarUrl,
                )
            }

            FollowListUiState(
                loading = false,
                title = if (mode == "followers") "Followers" else "Following",
                people = people,
                error = null,
            )
        }
    }
}
