package com.howmylook.app.data.reports

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.toFriendlyReportError
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ContentReportInsertDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("reason") val reason: String? = null,
)

class ContentReportRepository {
    suspend fun reportPost(config: SupabaseConfig, postId: String, reason: String?): Result<String> {
        return submitReport(config, targetType = "post", targetId = postId, reason = reason)
    }

    suspend fun reportProfile(config: SupabaseConfig, profileId: String, reason: String?): Result<String> {
        return submitReport(config, targetType = "profile", targetId = profileId, reason = reason)
    }

    private suspend fun submitReport(
        config: SupabaseConfig,
        targetType: String,
        targetId: String,
        reason: String?,
    ): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val reporterId = client.auth.retrieveUserForCurrentSession(updateSession = false).id
            if (reporterId.isBlank()) {
                error("Sign in before reporting content.")
            }

            client.from("content_reports").insert(
                ContentReportInsertDto(
                    reporterId = reporterId,
                    targetType = targetType,
                    targetId = targetId,
                    reason = reason?.trim()?.takeIf { it.isNotBlank() },
                )
            )

            "Report submitted. Thanks for helping keep HowMyLook safe."
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyReportError(it.message), it)) },
        )
    }
}
