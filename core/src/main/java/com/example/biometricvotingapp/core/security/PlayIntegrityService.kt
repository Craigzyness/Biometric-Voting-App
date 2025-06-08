package com.example.biometricvotingapp.core.security // Updated package

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber // Assuming Timber is available in :core or this import is adjusted
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

// Custom Exception for Play Integrity specific errors
// This class is now part of the com.example.biometricvotingapp.core.security package
class PlayIntegrityException(message: String, val errorCode: Int? = null, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class PlayIntegrityService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val NONCE_NUM_BYTES = 16
    }

    fun generateNonce(): String {
        val randomBytes = ByteArray(NONCE_NUM_BYTES)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    suspend fun requestIntegrityToken(nonce: String): Result<String> {
        if (nonce.isBlank()) {
            Timber.tag("PlayIntegrityService").e("Nonce cannot be blank.")
            return Result.failure(PlayIntegrityException("Nonce cannot be blank."))
        }

        val integrityManager = try {
            IntegrityManagerFactory.create(context)
        } catch (e: Exception) {
            Timber.tag("PlayIntegrityService").e(e, "Failed to create IntegrityManager.")
            return Result.failure(PlayIntegrityException("Failed to create IntegrityManager.", cause = e))
        }

        val tokenRequestBuilder = IntegrityTokenRequest.builder()
            .setNonce(nonce)
        val tokenRequest = tokenRequestBuilder.build()

        return try {
            Timber.tag("PlayIntegrityService").d("Requesting integrity token with nonce (first 10 chars): %s...", nonce.take(10))
            val integrityTokenResponse = integrityManager.requestIntegrityToken(tokenRequest).await()
            val token = integrityTokenResponse.token()
            if (token.isNullOrEmpty()) {
                Timber.tag("PlayIntegrityService").e("Received null or empty token.")
                Result.failure(PlayIntegrityException("Received null or empty integrity token."))
            } else {
                Timber.tag("PlayIntegrityService").i("Successfully received integrity token.")
                Result.success(token)
            }
        } catch (e: com.google.android.play.core.integrity.IntegrityServiceException) {
            Timber.tag("PlayIntegrityService").e(e, "IntegrityServiceException while requesting token. Code: %d (%s)", e.errorCode(), getErrorMessageForCode(e.errorCode()))
            Result.failure(PlayIntegrityException("Integrity check failed: ${getErrorMessageForCode(e.errorCode())}", errorCode = e.errorCode(), cause = e))
        } catch (e: Exception) {
            Timber.tag("PlayIntegrityService").e(e, "Generic exception while requesting token.")
            Result.failure(PlayIntegrityException("Integrity check failed due to an unexpected error.", cause = e))
        }
    }

    fun getErrorMessageForCode(errorCode: Int): String {
        return when (errorCode) {
            IntegrityErrorCode.NO_ERROR -> "No error."
            IntegrityErrorCode.API_NOT_AVAILABLE -> "Integrity API is not available on this device, or the Play Store version is too old."
            IntegrityErrorCode.NETWORK_ERROR -> "Network error. Please check your internet connection and try again."
            IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "The Google server is temporarily unavailable. Please try again later."
            IntegrityErrorCode.INTERNAL_ERROR -> "An internal error occurred in the Play Integrity API."
            IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> "The Play Store app is not found on the device, is disabled, or is outdated."
            IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> "No active Google Play account is found on the device."
            IntegrityErrorCode.APP_NOT_INSTALLED -> "The calling app is not installed on the device (e.g., in a test environment where Play Store cannot verify)."
            IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> "Google Play Services is not found on the device or its version is too old."
            IntegrityErrorCode.APP_UID_MISMATCH -> "The calling app's UID does not match the one inferred by Play Store."
            IntegrityErrorCode.TOO_MANY_REQUESTS -> "The app is making too many requests to the API. Try again later."
            IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> "Cannot bind to the Play Store service. This may be a temporary issue."
            IntegrityErrorCode.NONCE_TOO_SHORT -> "The nonce provided is too short. Minimum 16 bytes required after Base64 decoding."
            IntegrityErrorCode.NONCE_TOO_LONG -> "The nonce provided is too long. Maximum 500 bytes allowed after Base64 decoding."
            IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> "The nonce is not Base64 encoded, or the encoding is invalid."
            IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> "The Google Cloud Project number linked to the app is invalid (if provided)."
            IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> "A transient error occurred on the client side. Retrying the request may succeed."
            else -> "Unknown integrity error code: $errorCode"
        }
    }
}
