package com.howmylook.app.data

import com.howmylook.app.BuildConfig

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank()

    companion object {
        fun fromBuildConfig(): SupabaseConfig {
            return SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
            )
        }
    }
}
