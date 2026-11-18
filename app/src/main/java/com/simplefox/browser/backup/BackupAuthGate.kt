package com.simplefox.browser.backup

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

sealed class AuthGateResult {
    object Success : AuthGateResult()
    object Failed : AuthGateResult()
    object NotAvailable : AuthGateResult()
    data class Error(val message: String?) : AuthGateResult()
}

/**
 * Device authentication using only platform APIs.
 * API 30+: android.hardware.biometrics.BiometricPrompt (biometric or device credential).
 * API 26-29: system lock-screen confirmation (PIN / pattern / password).
 */
object BackupAuthGate {

    private const val ALLOWED_AUTHENTICATORS_API30 =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isAuthenticationAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(BiometricManager::class.java) ?: return false
            manager.canAuthenticate(ALLOWED_AUTHENTICATORS_API30) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        }
    }

    suspend fun authenticate(activity: ComponentActivity, title: String, subtitle: String): AuthGateResult {
        if (!isAuthenticationAvailable(activity)) return AuthGateResult.NotAvailable
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            authenticateWithPrompt(activity, title, subtitle)
        } else {
            authenticateWithKeyguard(activity, title, subtitle)
        }
    }

    private suspend fun authenticateWithPrompt(
        activity: ComponentActivity,
        title: String,
        subtitle: String
    ): AuthGateResult = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                if (continuation.isActive) continuation.resume(AuthGateResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                if (!continuation.isActive) return
                val result = when (errorCode) {
                    BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                    BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> AuthGateResult.Failed
                    else -> AuthGateResult.Error(errString?.toString())
                }
                continuation.resume(result)
            }

            override fun onAuthenticationFailed() {}
        }
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS_API30)
            .build()
        continuation.invokeOnCancellation { runCatching { signal.cancel() } }
        prompt.authenticate(signal, activity.mainExecutor, callback)
    }

    @Suppress("DEPRECATION")
    private suspend fun authenticateWithKeyguard(
        activity: ComponentActivity,
        title: String,
        subtitle: String
    ): AuthGateResult = suspendCancellableCoroutine { continuation ->
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        val intent = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent == null) {
            continuation.resume(AuthGateResult.NotAvailable)
            return@suspendCancellableCoroutine
        }
        var launcherRef: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null
        val launcher = activity.activityResultRegistry.register(
            "backup_auth_" + UUID.randomUUID(),
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            launcherRef?.unregister()
            if (continuation.isActive) {
                continuation.resume(
                    if (result.resultCode == android.app.Activity.RESULT_OK) AuthGateResult.Success
                    else AuthGateResult.Failed
                )
            }
        }
        launcherRef = launcher
        continuation.invokeOnCancellation { launcher.unregister() }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            launcher.unregister()
            if (continuation.isActive) continuation.resume(AuthGateResult.Error(e.message))
        }
    }
}
