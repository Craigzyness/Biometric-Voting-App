# Play Integrity API Setup Guide (Cloud & Play Console)

To use the Play Integrity API in the Biometric Voting App, several configurations are required in your Google Cloud Project and Google Play Console. These steps must be performed manually.

## Prerequisites

*   You have a Google Play Console account where the app will be published.
*   You have a Google Cloud Project.

## Steps

1.  **Link Google Cloud Project to Play Console:**
    *   Ensure your Google Cloud Project is linked to your Google Play Console account.
    *   In Play Console, go to **Setup > API access**.
    *   Under "Linked Google Cloud project," either link an existing project or create a new one. This project will be used for managing Play Integrity API settings.

2.  **Enable Play Integrity API in Google Cloud Project:**
    *   Go to the Google Cloud Console: [https://console.cloud.google.com/](https://console.cloud.google.com/)
    *   Select the Google Cloud Project that is linked to your Play Console.
    *   Navigate to **APIs & Services > Library**.
    *   Search for "Play Integrity API".
    *   Select it and click **Enable**.

3.  **Configure Play Integrity API Settings (Important for Backend Verification - Future Step):**
    *   While full backend verification of the integrity token is a future step for this project, it's good to be aware of these settings.
    *   In the Google Cloud Console, navigate to the Play Integrity API settings page (usually found under **APIs & Services > Enabled APIs & services > Play Integrity API > Integrity Codelab** or by searching for "Play Integrity API" and going to its dashboard).
    *   Here you can:
        *   **Monitor API usage:** Check quotas and traffic. Standard requests are free up to a certain limit (e.g., 10,000 requests/day). For higher usage, you might need to enable billing on your Cloud Project and request quota increases.
        *   **Device integrity settings:** Review and understand the types of device integrity verdicts (`MEETS_DEVICE_INTEGRITY`, `MEETS_BASIC_INTEGRITY`, `MEETS_STRONG_INTEGRITY`).
        *   **Response encryption (for backend):** For backend verification, Google recommends encrypting the verdict. This involves setting up encryption keys in your Cloud Project. *This project is not implementing backend verdict decryption in the current phase.*

4.  **Ensure App Signing is Managed by Google Play:**
    *   For Play Integrity to work reliably and for Google to provide the most accurate attestations, it's highly recommended to use Play App Signing.
    *   If you're not already using it, enroll your app in Play App Signing in the Play Console (**Setup > App integrity > App signing**).

5.  **Testing Play Integrity:**
    *   **Local Testing:** For basic client-side integration testing, you can often get a token. However, the `deviceIntegrity` verdict might be limited or behave differently than on a production Play Store installation.
    *   **Play Console Testing Tracks:** To test Play Integrity more thoroughly, including different device verdicts, upload your app to internal, closed, or open testing tracks in the Play Console. This allows Google Play services to provide more accurate attestations for test devices.
    *   **Firebase App Distribution:** Can also be used, but ensure the app version matches what Play services expect.
    *   Refer to Google's documentation on testing strategies for Play Integrity.

## Important Notes

*   **API Quotas:** Be mindful of the standard request limits. If you anticipate high traffic, especially once backend verification is implemented, monitor usage and request quota increases if necessary.
*   **Nonce:** The Android client will generate a nonce for each request. If you implement backend verification later, this nonce should ideally be generated or bound by your backend to ensure freshness and prevent replay attacks effectively.
*   **Backend Verification (Future):** The current phase only implements client-side token requests. For full security, the token **must** be sent to your trusted backend, and your backend must verify it with Google's servers. This protects against client-side tampering and ensures the verdict is trustworthy.

## Official Documentation

Always refer to the official Google documentation for the most up-to-date and detailed instructions:
*   **Play Integrity API Overview:** [https://developer.android.com/google/play/integrity/overview](https://developer.android.com/google/play/integrity/overview)
*   **Enable and Configure Play Integrity:** [https://developer.android.com/google/play/integrity/setup](https://developer.android.com/google/play/integrity/setup)
*   **Test your Play Integrity integration:** [https://developer.android.com/google/play/integrity/test](https://developer.android.com/google/play/integrity/test)

This guide provides a starting point. Ensure you understand the implications of each setting for your application's security and user experience.
```
