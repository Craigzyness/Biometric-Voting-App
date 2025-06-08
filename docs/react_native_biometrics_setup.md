# React Native Biometrics: Native Configuration Steps

This document outlines the typical native configuration steps required after adding `react-native-biometrics` to your React Native project. Always refer to the official library documentation for the most up-to-date instructions.

## 1. Install the Package

Ensure you have added `react-native-biometrics` to your `package.json` and run:
```bash
npm install
# or
yarn install
```

## 2. iOS Configuration

### 2.1. Link Native Modules
   - For React Native 0.60 and above, auto-linking should handle most of it.
   - However, you still need to install the pods:
     ```bash
     cd ios
     pod install
     cd ..
     ```

### 2.2. Add Face ID Usage Description (Info.plist)
   - Open your project's `ios/<YourProjectName>/Info.plist` file.
   - Add the `NSFaceIDUsageDescription` key with a string value explaining why your app requires Face ID. For example:
     ```xml
     <key>NSFaceIDUsageDescription</key>
     <string>Enable Face ID for quick and secure access to your account and voting features.</string>
     ```
   - If you don't add this, your app might crash when trying to use Face ID.

## 3. Android Configuration

### 3.1. Link Native Modules
   - Auto-linking should generally work for React Native 0.60+.

### 3.2. Add Biometric Permission (AndroidManifest.xml)
   - Open your project's `android/app/src/main/AndroidManifest.xml` file.
   - Add the `USE_BIOMETRIC` permission:
     ```xml
     <manifest ...>
         <uses-permission android:name="android.permission.USE_BIOMETRIC"/>
         ...
     </manifest>
     ```
   - For older Android versions (API < 28), you might also need `USE_FINGERPRINT`:
     ```xml
     <uses-permission android:name="android.permission.USE_FINGERPRINT"/>
     ```
     The `react-native-biometrics` library often handles this gracefully, but it's good to be aware.

### 3.3. Update `android/build.gradle` (If necessary)
   - Ensure your project's `android/build.gradle` has a high enough `minSdkVersion`. The library documentation will specify requirements. Often, API level 23 (Android M) or higher is needed for full fingerprint support, and API 28 (Android P) for the BiometricPrompt API.
     ```gradle
     // In android/build.gradle
     buildscript {
         ext {
             minSdkVersion = 23 // Or higher, as per library requirements
             // ... other configurations
         }
         // ...
     }
     ```

### 3.4. Update `MainApplication.java` or `MainActivity.java` (Less common now)
   - For very old React Native versions or specific library versions, manual registration of the package might have been needed. With auto-linking, this is usually not required. Always check the library's specific installation guide.

## 4. Rebuild Your App
After these changes, rebuild your application for the changes to take effect:
```bash
npx react-native run-android
npx react-native run-ios
```

## Important Notes:
- **Emulator/Device Support:** Ensure your testing device or emulator has biometric capabilities enabled and configured.
- **Permissions Handling:** While these steps set up the permissions, your app code should also handle requesting permissions at runtime if necessary, though `react-native-biometrics` often manages the prompt itself.
- **Library Version:** These instructions are general. Specific versions of `react-native-biometrics` might have slightly different or additional steps. **Always consult the official documentation for the version you are using.**

This guide provides a starting point for the native setup.
