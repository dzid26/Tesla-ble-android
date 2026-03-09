# Tesla BLE Android

Android app scaffold to discover a Tesla over BLE, connect, and send an initial authentication frame directly from the phone.

## What is implemented

- BLE permission handling for Android 8+ / 12+.
- Tesla-targeted BLE scan (service UUID and name heuristic).
- BLE GATT connect + service discovery.
- Initial authentication frame write pathway for Tesla command channel.
- GitHub Actions workflow to build debug APK and publish release APK artifact on tag.

## What still needs production hardening

- Replace placeholder auth payload with Tesla Vehicle Command protobuf messages.
- Generate and securely store device keys in Android Keystore.
- Implement challenge/response signing flow and session management.
- Parse notifications from the vehicle and handle retries / reconnects.
- Add command layer (unlock, climate, trunk, etc.) with user consent and auditing.

## Local build

```bash
gradle :app:assembleDebug
```

## GitHub release flow

1. Push code to GitHub.
2. Create and push a tag like `v0.1.0`.
3. Workflow builds APK and uploads it to GitHub Release assets.

