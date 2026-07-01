package com.howmylook.app.data.auth

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.toFriendlyPasswordResetError
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class RequestPasswordResetResponse(
    @SerialName("sent") val sent: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
)

class PasswordResetRepository {
    suspend fun requestPasswordReset(config: SupabaseConfig, email: String): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val response = client.functions.invoke(
                function = "request-password-reset",
                body = buildJsonObject {
                    put("email", email.trim())
                },
            ).body<RequestPasswordResetResponse>()

            if (response.error != null) {
                error(response.error)
            }
            if (!response.sent) {
                error("Unable to send password reset email.")
            }
            response.message
                ?: "If an account exists for that email, password reset instructions were sent. Check your inbox and spam folder."
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyPasswordResetError(it.message), it)) },
        )
    }
}
