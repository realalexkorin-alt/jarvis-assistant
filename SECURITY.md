# Security and signing

The initial application requests network access for GitHub Releases and declares REQUEST_INSTALL_PACKAGES only for user-triggered APK installation. Android requires explicit approval for installation from this app and confirmation of each APK. No microphone, notifications, contacts, location, or Bluetooth permissions at this stage.

The release download must have the GitHub asset SHA-256 digest, a matching application package name and the matching Android signing identity for an in-place update. Keep the GitHub account and release signing key secure. A SHA checksum alone does not establish who published an APK.

## Configure production signing

In **Repository Settings > Secrets and variables > Actions**, create these repository secrets:

1. `JARVIS_KEYSTORE_BASE64`: one-line base64 contents of the private JKS signing keystore file.
2. `JARVIS_KEYSTORE_PASSWORD`: keystore password.
3. `JARVIS_KEY_ALIAS`: key alias (e.g. jarvis-release).
4. `JARVIS_KEY_PASSWORD`: key password.

Generate the keystore *privately* with Android Studio or `keytool -genkeypair -v -keystore jarvis-release.jks -alias jarvis-release -keyalg RSA -keysize 3072 -validity 10000`. Never add a JKS or passwords to public GitHub or app APK. Keep a private offline backup: losing the key prevents updating previously installed packages.

Release workflow fails closed if required signing secrets are absent. WAV references remain outside the public repo.
