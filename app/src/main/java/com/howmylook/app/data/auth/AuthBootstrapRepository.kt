package com.howmylook.app.data.auth

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.domain.AppStep
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first

class AuthBootstrapRepository(
    private val authRepository: AuthRepository = AuthRepository(),
) {
    suspend fun bootstrap(config: SupabaseConfig): SessionBootstrap {
        if (!config.isConfigured) {
            return SessionBootstrap(
                step = AppStep.AUTH,
                isConfigured = false,
                isSignedIn = false,
                message = "Supabase config missing.",
            )
        }

        val client = SupabaseProvider.create(config)
        val sessionStatus = client.auth.sessionStatus.first()
        val isSignedIn = sessionStatus is SessionStatus.Authenticated

        if (!isSignedIn) {
            return SessionBootstrap(
                step = AppStep.AUTH,
                isConfigured = true,
                isSignedIn = false,
                message = "Sign in to continue.",
            )
        }

        val profile = authRepository.loadCurrentProfile(config).getOrNull()
        val userId = profile?.id
        if (userId != null) {
            authRepository.resetLoginRatingCounter(config, userId)
        }
        val availablePostCount = if (userId != null) {
            authRepository.getAvailableRatingPostCount(config, userId).getOrDefault(0)
        } else {
            0
        }

        val step = authRepository.resolveStep(
            isAuthenticated = true,
            profile = profile,
            availablePostCount = availablePostCount,
        )

        return SessionBootstrap(
            step = step,
            isConfigured = true,
            isSignedIn = true,
            profile = profile,
            availablePostCount = availablePostCount,
            message = when (step) {
                AppStep.AUTH -> "Sign in to continue."
                AppStep.USERNAME -> "Signed in. Choose a username to continue."
                AppStep.RATING -> "Signed in. Rate 5 looks to unlock the app."
                AppStep.UNLOCKED -> "Signed in. Home is ready."
            },
        )
    }
}
