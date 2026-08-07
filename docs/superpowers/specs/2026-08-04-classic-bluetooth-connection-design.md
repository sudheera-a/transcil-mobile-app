# Classic Bluetooth connection screen — design

**Date:** 2026-08-04  
**App:** TranscilMobileApp  
**Status:** Approved in chat; awaiting file review before implementation plan

## Goal

Add a simple Classic Bluetooth screen under Settings: toggle adapter on/off, list paired and nearby devices, connect over SPP (RFCOMM), send/receive plain text in a serial console, keep the link alive when the UI is gone via a foreground service, and attempt reconnect to the last device after process death on next app open.

## Decisions

| Topic | Decision |
|-------|----------|
| Bluetooth type | Classic Bluetooth (not BLE) |
| Profile / UUID | Standard SPP `00001101-0000-1000-8000-00805F9B34FB` |
| Entry point | Profile → Settings → Bluetooth row → `BluetoothFragment` |
| Device lists | Two sections: **Paired** and **Available / nearby** |
| Post-connect UI | Serial console: text field + Send + scrollable TX/RX log |
| Connection ownership | Foreground `BluetoothConnectionService` owns the socket |
| Background | Connection stays up when user leaves screen / presses Home (notification required) |
| Process death | Persist last device MAC (+ name); on next open, service attempts reconnect |
| Force-stop limit | Android kills the process; reconnect only after user opens the app again |
| Architecture style | Existing MVVM + ViewBinding + LiveData; no Hilt, no Compose |
| Package | `com.transcil.rider` (e.g. `...bluetooth` package) |
| Commits | Only when explicitly requested |

## In scope (v1)

1. Settings row + `BluetoothFragment` + layout matching existing Settings sub-screen patterns (back, bottom nav hidden).
2. Runtime permissions for Classic BT (legacy + API 31+ connect/scan; location for discovery on older APIs; notification permission for FGS on API 33+).
3. Bluetooth on/off toggle that reflects adapter state and requests enable via system UI.
4. Paired device list from `BluetoothAdapter.bondedDevices`.
5. Discovery scan for available devices; Stop scan; merge into Available list (exclude already-paired duplicates if shown in Paired).
6. Tap device → connect via RFCOMM SPP in the foreground service.
7. Status line: Off / On / Scanning / Connecting / Connected to \<name\> / Disconnected / Error.
8. Serial console when connected: Send writes bytes; read loop appends RX; UI logs TX/RX lines.
9. Disconnect action; only one active connection at a time.
10. Persist last connected device; recover/reconnect attempt after process death when app returns.
11. Foreground notification while connected (or connecting/reconnecting as needed for FGS rules).
12. One small self-check/unit test for last-device save/restore helper.

## Out of scope (v1)

- BLE / GATT
- Multi-device simultaneous connections
- Custom UUID picker
- File transfer or structured binary protocol UI
- Companion Device Manager
- Keeping connection alive through user force-stop without reopening the app
- Auto-scan forever with no user action

## Architecture

| Piece | Responsibility |
|-------|----------------|
| `BluetoothFragment` | UI: toggle, lists, console, permission/enable prompts |
| `BluetoothViewModel` | UI state; forwards scan/connect/send/disconnect to service/repo; LiveData |
| `BluetoothConnectionService` | Owns `BluetoothSocket`, connect/read/write threads, reconnect, FGS notification |
| `BluetoothRepository` | Adapter helpers: enable state, bonded devices, discovery broadcasts, last-device prefs |
| `BluetoothDeviceAdapter` | RecyclerView rows for paired + available |
| Last-device prefs | Tiny store for address (+ display name) used for recovery |

**Ownership rule:** UI never holds the socket. The service does. Fragment/ViewModel destruction must not close an intentional live connection.

## Screen layout

Top → bottom:

1. Title + back
2. Bluetooth On/Off switch
3. Connection status line
4. Paired devices list
5. Available devices list + Scan / Stop scan
6. When connected: serial console (log, input, Send, Disconnect)

## Data flow

```text
Settings → BluetoothFragment
  → permissions + adapter ON/OFF
  → Scan → discovery broadcast → Available list
  → Tap device → ViewModel → Service.connect(address)
       → RFCOMM SPP socket on worker thread
       → success: save MAC, start read loop, FGS notification
       → failure: Error status
  → Send → Service.write → TX log
  → Read loop → RX → ViewModel → RX log
  → UI gone → Service keeps socket
  → Process death → next open reads saved MAC → Service.reconnect()
```

## Permissions & manifest

- Legacy: `BLUETOOTH`, `BLUETOOTH_ADMIN`; location as required for discovery on older APIs
- API 31+: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (with appropriate `usesPermissionFlags` / neverForLocation if applicable)
- Foreground service type: `connectedDevice` (Classic BT link while UI is gone)
- `POST_NOTIFICATIONS` on API 33+ for the connected notification
- Declare `BluetoothConnectionService` in the manifest with that FGS type

Exact permission strings/flags are finalized in the implementation plan against `minSdk 24` / `targetSdk 36`.

## Error handling

- Unsupported BT → message; controls disabled
- Permission denied → explain; optional open app settings; no crash
- Adapter off → lists empty until enabled
- Discovery timeout → stop scan; keep found devices
- Connect fail / out of range → Error + short toast; keep last MAC for retry
- Socket drop → Disconnected; disable Send until reconnected
- New connect while connected → disconnect previous first

## Verification

Manual:

1. Toggle BT on/off from the screen  
2. See paired devices; scan shows available  
3. Connect to an SPP device (or phone serial SPP test app)  
4. Send text; see TX/RX in log  
5. Press Home — connection stays; notification visible  
6. Swipe away app — reopen → reconnect attempt / status  
7. Disconnect clears active link and updates UI  

Automated:

- One small test/self-check for last-device prefs save/restore

## Implementation order (for later plan; user codes step by step)

1. Manifest permissions + Settings row + empty `BluetoothFragment`  
2. Toggle + permission flow + adapter state  
3. Paired list  
4. Discovery / Available list  
5. Service + connect/disconnect + status  
6. Serial console TX/RX  
7. Last-device persist + reconnect on start  
8. Manual verification pass  

## Honest platform limit

Force-stop ends the process and service. v1 recovers by reconnecting when the user opens the app again; it does not claim to stay linked through force-stop while the app never returns.
