package com.howmylook.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {
    fun create(config: SupabaseConfig): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.anonKey,
        ) {
            defaultSerializer = KotlinXSerializer()
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
