# Firebase Setup Guide

This guide will help you configure Firebase for Auralis Music. Firebase is required for certain features like analytics, crash reporting, and cloud messaging.

## Prerequisites

- A Firebase account (free tier is sufficient)
- Google Cloud Platform project
- Android Studio or Android SDK installed

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter your project name (e.g., "Auralis Music")
4. Follow the setup wizard
5. Enable Google Analytics if desired (optional)

## Step 2: Add Android App

1. In your Firebase project dashboard, click "Add app"
2. Select Android as the platform
3. Enter your app details:
   - **Package name**: `com.auralismusic.app` (check your `app/build.gradle` for the actual package name)
   - **App nickname**: Auralis Music (optional)
   - **Debug signing key SHA-1**: Run this command to get it:
     ```bash
     keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
     ```
4. Click "Register app"
5. Download the `google-services.json` file

## Step 3: Add Firebase Configuration File

1. Place the downloaded `google-services.json` file in the `app/` directory of your project:
   ```
   AuralisMusic467/
   ├── app/
   │   ├── google-services.json  ← Place this file here
   │   ├── src/
   │   └── build.gradle.kts
   └── ...
   ```

## Step 4: Configure Gradle

The Firebase dependencies are already included in the project, but ensure your `app/build.gradle.kts` contains:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // This line should be present
}

dependencies {
    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")
}
```

## Step 5: Enable Firebase Services

In the Firebase Console, enable the services you want to use:

### Analytics (Optional)
- Go to Analytics → Dashboard
- Analytics will start collecting data automatically

### Crashlytics (Recommended)
- Go to Crashlytics → Dashboard
- Crash reports will be collected automatically


## Step 6: Build and Test

Build your app to verify Firebase integration:

```bash
./gradlew clean :app:assembleUniversalFossDebug
```

Install and run the app. Check the Firebase Console dashboard to see if data is being collected.

## Troubleshooting

### Build Errors
- Ensure `google-services.json` is in the correct location (`app/` directory)
- Verify the package name matches your Firebase project
- Check that the Google Services plugin is applied

### No Data in Console
- Make sure your app has internet connectivity
- Check that the correct SHA-1 certificate was added
- Verify Firebase services are enabled in the console

### Release Builds
For release builds, you'll need to add your release signing key SHA-1:
```bash
keytool -list -v -keystore path/to/your/release.keystore -alias your-alias
```

## Optional Features

### Firebase Performance Monitoring
Add to your `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-perf")
```

### Firebase Remote Config
Add to your `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-config")
```

## Security Notes

- Never commit `google-services.json` to version control
- Add `google-services.json` to your `.gitignore` file
- Use different Firebase projects for development and production
- Regularly rotate your API keys and certificates

## Support

If you encounter issues:
1. Check the [Firebase documentation](https://firebase.google.com/docs)
2. Review the [Android integration guide](https://firebase.google.com/docs/android/setup)
3. Open an issue on our [GitHub repository](https://github.com/iankr347/AuralisMusic467/issues)

---

**Note**: Firebase setup is optional for basic functionality. The app will work without Firebase, but some features like analytics and crash reporting won't be available.

