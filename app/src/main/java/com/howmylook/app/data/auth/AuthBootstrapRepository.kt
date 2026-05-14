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
                debugMessage = "Bootstrap stopped: missing Supabase config.",
            )
        }

        return runCatching {
            val client = SupabaseProvider.create(config)
            val sessionStatus = client.auth.sessionStatus.first()
            val isSignedIn = sessionStatus is SessionStatus.Authenticated

            if (!isSignedIn) {
                return SessionBootstrap(
                    step = AppStep.AUTH,
                    isConfigured = true,
                    isSignedIn = false,
                    message = "Sign in to continue.",
                    debugMessage = "Bootstrap: session status is ${sessionStatus::class.simpleName ?: "unknown"}.",
                )
            }

            val profileResult = authRepository.loadCurrentProfile(config)
            val profile = profileResult.getOrElse { error ->
                return SessionBootstrap(
                    step = AppStep.AUTH,
                    isConfigured = true,
                    isSignedIn = true,
                    message = "Signed in, but we couldn't load your account yet.",
                    debugMessage = "Bootstrap profile load failed: ${error.message ?: error::class.simpleName ?: "unknown error"}",
                )
            }

            val userId = profile?.id ?: return SessionBootstrap(
                step = AppStep.AUTH,
                isConfigured = true,
                isSignedIn = true,
                message = "Signed in, but your account session is incomplete.",
                debugMessage = "Bootstrap profile load returned null user id.",
            )

            val resetError = authRepository.resetLoginRatingCounter(config, userId).exceptionOrNull()
            val availablePostCountResult = authRepository.getAvailableRatingPostCount(config, userId)
            val availablePostCount = availablePostCountResult.getOrElse { 0 }

            val step = authRepository.resolveStep(
                isAuthenticated = true,
                profile = profile,
                availablePostCount = availablePostCount,
            )

            SessionBootstrap(
                step = step,
                isConfigured = true,
                isSignedIn = true,
                profile = profile,
                availablePostCount = availablePostCount,
                message = when (step) {
                    AppStep.AUTH -> "Signed in, but setup is incomplete."
                    AppStep.USERNAME -> "Signed in. Choose a username to continue."
                    AppStep.RATING -> "Signed in. Rate 5 looks to unlock the app."
                    AppStep.UNLOCKED -> "Signed in. Home is ready."
                },
                debugMessage = buildString {
                    append("Bootstrap ok: step=")
                    append(step.name)
                    append(", userId=")
                    append(userId)
                    append(", availablePostCount=")
                    append(availablePostCount)
                    if (resetError != null) {
                        append(", resetLoginRatingCounterFailed=")
                        append(resetError.message ?: resetError::class.simpleName ?: "unknown")
                    }
                    availablePostCountResult.exceptionOrNull()?.let { error ->
                        append(", availablePostCountFailed=")
                        append(error.message ?: error::class.simpleName ?: "unknown")
                    }
                },
            )
        }.getOrElse { error ->
            SessionBootstrap(
                step = AppStep.AUTH,
                isConfigured = true,
                isSignedIn = false,
                message = "Couldn't restore your sign-in yet.",
                debugMessage = "Bootstrap crashed: ${error.message ?: error::class.simpleName ?: "unknown error"}",
            )
        }
    }
}
