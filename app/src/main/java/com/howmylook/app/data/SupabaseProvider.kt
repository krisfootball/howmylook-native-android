package com.howmylook.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {
    @Volatile
    private var cachedConfig: SupabaseConfig? = null

    @Volatile
    private var cachedClient: SupabaseClient? = null

    fun create(config: SupabaseConfig): SupabaseClient {
        val existingClient = cachedClient
        val existingConfig = cachedConfig
        if (existingClient != null && existingConfig == config) {
            return existingClient
        }

        return synchronized(this) {
            val syncedClient = cachedClient
            val syncedConfig = cachedConfig
            if (syncedClient != null && syncedConfig == config) {
                syncedClient
            } else {
                createSupabaseClient(
                    supabaseUrl = config.url,
                    supabaseKey = config.anonKey,
                ) {
                    defaultSerializer = KotlinXSerializer()
                    install(Auth)
                    install(Postgrest)
                    install(Storage)
                    install(Functions)
                }.also { client ->
                    cachedConfig = config
                    cachedClient = client
                }
            }
        }
    }
}
