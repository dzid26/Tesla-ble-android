# Tesla BLE Android

Android app scaffold to discover a Tesla over BLE, authenticate, monitor vitals, and set charging current directly from the phone.

## Implemented now

- BLE permission handling for Android 8+ / 12+.
- BLE readiness checks before actions:
  - Bluetooth must be enabled.
  - Required runtime permissions must be granted.
  - User action is resumed automatically after permission grant.
- Tesla-targeted BLE scan using service UUID scan filter + low-latency scan settings.
- BLE GATT connect + MTU request + service discovery + notification subscription.
- Initial authentication frame write pathway for Tesla command channel.
- Command stubs for:
  - Requesting vehicle vitals.
  - Setting charging current in amps.
- Grid-aware charging current controller:
  - Inputs: grid voltage + kW setpoint + max amps.
  - Computes amps and sends charge current command.
- GitHub Actions workflow:
  - Builds + unit tests on every pull request.
  - Builds APK and publishes release asset on `v*` tags.

## Important production work still required

- Replace placeholder command frames with real Tesla Vehicle Command protobuf payloads.
- Implement official Tesla BLE challenge/response cryptography and key exchange.
- Persist keys in Android Keystore and support key enrollment/rotation.
- Parse real Tesla telemetry payloads instead of the temporary frame format.
- Add command authorization UX and safety limits.

## Local build

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## GitHub release flow

1. Push to GitHub and open a PR (build + unit tests run automatically).
2. Merge to your target branch.
3. Create and push a tag like `v0.2.0`.
4. Workflow builds APK and uploads it to GitHub Release assets.

## GitHub access expectations

This project can be prepared locally (commits + PR metadata), but direct GitHub operations require environment setup:

- `origin` remote configured for this repository
- credentials/token with push + PR permissions
- optional: GitHub CLI (`gh`) installed and authenticated

If those are missing, the code can still be fully prepared and validated locally/CI, then pushed from a configured machine.

