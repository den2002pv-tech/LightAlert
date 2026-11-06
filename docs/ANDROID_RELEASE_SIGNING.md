# Android release signing and in-place updates

LightAlert keeps the application ID `app.lightalert`. Android can install a newer APK over an existing installation and preserve app data only when the new APK is signed with the same signing key and has a higher `versionCode`.

## Create the permanent upload key once

Keep this keystore private and backed up in a secure location. Never commit it to Git.

Run once on a trusted computer:

```bash
keytool -genkeypair -v -keystore lightalert-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lightalert
```

Use a strong keystore password and key password. Do not lose this file: all future APK updates must use this exact key.

Encode the keystore as Base64:

- Linux/macOS: `base64 -w 0 lightalert-upload.jks`
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("lightalert-upload.jks"))`

## Add GitHub Actions secrets

In the repository, open Settings → Secrets and variables → Actions → New repository secret. Add:

- `LIGHTALERT_KEYSTORE_BASE64` — Base64 output from the keystore file
- `LIGHTALERT_KEYSTORE_PASSWORD` — keystore password
- `LIGHTALERT_KEY_ALIAS` — `lightalert` (or the alias chosen above)
- `LIGHTALERT_KEY_PASSWORD` — key password

After all four secrets are present, pushes to `feature/android-foundation` build both the debug APK and a signed release APK artifact named `LightAlert-release-apk`.

## Installing updates

Install the signed release APK. Later releases must keep application ID `app.lightalert`, use the same keystore and alias, and increase `versionCode` in `app/build.gradle.kts`. Android's package installer will then offer an update rather than requiring uninstall; app data remains in place.

Debug APKs are for testing only. GitHub-hosted runners create a temporary debug signing key, so debug artifacts from different CI runs are not a reliable update channel. Do not install a debug APK as the permanent version if you intend to receive signed release updates.

The first transition from an already-installed APK signed with a different key cannot be performed as an in-place update by Android. Plan that signing-key transition before distributing the app widely.
