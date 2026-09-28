# SmartSolarStationsApp — Member 2 Android Module (Stations/Map)

Native Kotlin + XML demo. **Stations/map only** — auth shell is a thin demo passthrough.
Reservations (Member 3), QR verify (Member 4), users/profile (Member 1) plug in later.

## Run
1. Start API: `dotnet run` in `Backend/SmartSolarMicrogridAPI` → `http://localhost:5205`.
2. Open `Mobile/SmartSolarStationsApp` in Android Studio Hedgehog+.
3. Run on emulator (API 34). `BuildConfig.API_BASE_URL` = `http://10.0.2.2:5205` (emulator → host).
4. Login with `200012345678 / 123456` (Prosumer) or `199512345678 / 123456` (Operator).

## What works offline
Live `GET /api/solarstations` first, fallback to `MockData` (ST001–ST005) + SQLite `StationCache`.
Mock map needs no API key. QR scan is stubbed; `QrDemoHelper` generates demo codes only.

## Ownership
Only files under `Mobile/` belong to Member 2. Never edit `Frontend/*` or other members'
backend files. Backend routes used: `/api/solarstations`, `/api/health`.

## Demo (30s)
Splash → Login (demo button) → Stations list (search/filter/stats) → Detail (mock map dots) → Book stub toast.
