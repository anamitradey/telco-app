# Feasibility: third-party Android SDK

Scope: a **Play-distributed library** inside a host app, public Android APIs, user-granted permissions. Not a privileged carrier app, not Device Owner / MDM, not the default dialer/SMS app.

Legend: **yes** = implemented in `:sdk` · **host** = SDK policy only, host must hold a system role · **carrier** = BSS/MNO · **no** = Play/platform blocked for this class of SDK

## Device / telemetry (blue)

| Feature | Verdict | Notes |
|---|---|---|
| Hotspot detection | **yes** | Soft-AP state broadcast. Cannot see clients on the hotspot or another phone’s SIM. |
| Wi-Fi SSID fingerprint | **yes** | Connected SSID/BSSID + host-supplied OUI list. Needs location / `NEARBY_WIFI_DEVICES`. Device MAC is randomized. Do not use for undisclosed competitor-home profiling (Play + privacy). |
| Dual SIM | **yes** | `SubscriptionManager` + `READ_PHONE_STATE`. Per-SIM byte counters need usage-access, not a silent hook. |
| Screen unlock | **yes** | Dynamic `ACTION_USER_PRESENT` while process is alive. |
| Battery / charging | **yes** | Sticky battery intent; charging+idle inferred. |
| App foreground | **yes** | Host app only via process lifecycle. Other apps = usage-access. |
| Signal strength | **yes** | `TelephonyCallback` + `READ_PHONE_STATE`. |

## Safety / parental (red)

| Feature | Verdict | Notes |
|---|---|---|
| Safety Circle + consent | **yes** | Local members. Fan-out SMS/push is host backend. |
| Unreachable / no network | **yes** | Local connectivity + last location. Phone-off needs server heartbeat. |
| Battery low / phone off | **yes** | Low battery local; power-off = stale heartbeat. |
| No activity | **yes** | Last unlock + last Wi-Fi change. |
| Broken routine + STB + wearable | **partial** | Unlock/Wi-Fi only. STB/wearable are other devices. |
| Network location + GPS | **yes** | GPS + network providers. Consent required. Background location is a Play declaration. |
| Landing abroad | **yes** | SIM vs network country / roaming. Notify family via callback. |
| SOS with live location | **yes** | User-initiated. OEM power-button SOS is not injectable. |
| Live location for parent | **yes** | Child publishes; parent UI is host. |
| Geofence / safe routes | **yes** | In-process distance checks (no Play Services). |
| SIM swap approval | **partial** | Detect ICCID change. Cannot approve/block a port. |
| Location granted by default | **no** | OS always prompts. |
| Emergency SOS linked to parents | **yes** | Same as SOS + consented members. |
| Spam caller block | **host** | `CallScreeningService` / default dialer. |
| Content filter / safe browsing | **host** | Host `VpnService` + blocklist in SDK. |
| Internet pause / study / bedtime / cap | **host** | Policy in SDK; enforce via sample VPN. App-level blocks need MDM. |
| Trusted contacts only | **host** | Allow-list stored; enforce needs dialer/SMS role. |
| App category control | **no** | `QUERY_ALL_PACKAGES` + hide-app is MDM / Play-restricted. |
| Premium / VAS / international block | **carrier** | Operator BSS. |
| Roaming control | **partial** | Detect and warn. Cannot detach roaming from a normal app. |
| Device security | **yes** | Root/emulator/adb heuristics, not a malware scanner. |
| Family dashboard | **yes** | Local profiles. Account/SIM provisioning is host/MNO. |
| Digital wellbeing report | **yes** | SDK event log; cross-app time needs usage-access. |
| Teen driving mode | **yes** | Speed heuristic; DND/SMS auto-reply need extra roles. |
| Fraud: OTPs / long unknown calls | **no** | SMS and call-log are Play-restricted outside default SMS/dialer. |
| Fraud combo + remote-access apps | **no** | Same, plus Accessibility policy. |
| Parental approval for number porting | **carrier** | |
| Block high-risk SPAM | **host** | See spam caller. |

## Permissions the host must request

`READ_PHONE_STATE`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES` (API 33+), optional `ACCESS_BACKGROUND_LOCATION`, VPN consent for pause, usage-access for other-app time.

## Implementation map

Implemented capabilities have `implemented=true` in `CapabilityCatalog`. The sample **Feasibility** tab is the live version of this table.
