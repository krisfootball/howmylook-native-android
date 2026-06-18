package com.howmylook.app.data.post

import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import java.time.Instant

fun PostgrestFilterBuilder.onlyNonExpiredPosts(nowIso: String = Instant.now().toString()) {
    or {
        eq("keep_forever", true)
        gt("expires_at", nowIso)
    }
}
