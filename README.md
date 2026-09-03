# Family Safety SDK

Third-party Android library plus sample UI. See **Feasibility** below before using any of these APIs — several items in the original spec cannot be done from a Play-distributed SDK.

## Modules

- `:sdk` — `SafetySdk` (device signals, safety circle, geofence/SOS, parental *policy*, security heuristics)
- `:sample` — operator UI (Feasibility / Signals / Safety / Parental)

## What this SDK will not do

- Read bank OTPs or call logs
- Block other apps, hide packages, or intercept SIM porting at the carrier
- Silent competitor-home tracking without a disclosed permission purpose
- Pause the whole device’s internet without the host app’s `VpnService` consent dialog

## Build

Open in Android Studio (JDK 17) or:

```
./gradlew :sdk:test :sample:assembleDebug
```
