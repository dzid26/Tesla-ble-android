# Tesla BLE Android

Android app scaffold to discover a Tesla over BLE, authenticate, monitor vitals, and set charging current directly from the phone.

## Implemented now

- BLE permission handling for Android 8+ / 12+.
- Tesla-targeted BLE scan (service UUID and name heuristic).
- BLE GATT connect + service discovery + notification subscription.
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
