package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.toFriendlyAccountDeletionError
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class DeleteAccountResponse(
    @SerialName("deleted") val deleted: Boolean = false,
    @SerialName("error") val error: String? = null,
)

class AccountDeletionRepository {
    suspend fun deleteAccount(config: SupabaseConfig): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val response = client.functions.invoke(function = "delete-account").body<DeleteAccountResponse>()
            if (response.error != null) {
                error(response.error)
            }
            if (!response.deleted) {
                error("Account deletion did not complete.")
            }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyAccountDeletionError(it.message), it)) },
        )
    }
}
