package com.howmylook.app.data.notifications

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
private data class AndroidPushDeviceDto(
    @SerialName("user_id") val userId: String,
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String = "android",
    @SerialName("app_id") val appId: String = "com.howmylook.app",
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String,
)

class AndroidPushRepository {
    suspend fun registerToken(
        config: SupabaseConfig,
        userId: String,
        token: String,
        deviceName: String,
    ): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("android_push_devices").upsert(
                AndroidPushDeviceDto(
                    userId = userId,
                    token = token,
                    deviceName = deviceName,
                    lastSeenAt = Instant.now().toString(),
                )
            )
        }
    }

    suspend fun removeToken(
        config: SupabaseConfig,
        userId: String,
        token: String,
    ): Result<Unit> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("android_push_devices").delete {
                filter {
                    eq("user_id", userId)
                    eq("token", token)
                }
            }
        }
    }
}
