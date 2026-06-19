package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.toFriendlyAccountDeletionError
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RequestAccountDeletionResponse(
    @SerialName("sent") val sent: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
)

class AccountDeletionRepository {
    suspend fun requestAccountDeletion(config: SupabaseConfig): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val response = client.functions.invoke(function = "request-account-deletion")
                .body<RequestAccountDeletionResponse>()
            if (response.error != null) {
                error(response.error)
            }
            if (!response.sent) {
                error("Unable to send account deletion email.")
            }
            response.message ?: "Confirmation email sent. Open the link in that email to permanently delete your account."
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyAccountDeletionError(it.message), it)) },
        )
    }
}
