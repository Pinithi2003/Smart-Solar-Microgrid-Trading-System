# SmartSolarMicrogridMobile — Android app (Members 1–4)

Native Kotlin + XML. Feature packages under `app/src/main/java/com/smartsolar/stations/`:

- `auth/` — Member 1 (Login, Registration, JWT, role-based access, Profile)
- `stations/` — Member 2 (Solar Stations, Google Maps)
- `reservations/` — Member 3 (Booking, Reservations)
- `fieldops/` — Member 4 (QR verify, field ops)
- `core/` — shared demo data + splash bootstrap

## Run
1. Start API: `dotnet run` in `Backend/SmartSolarMicrogridAPI` → `http://localhost:5205`.
2. Open `Mobile/SmartSolarMicrogridMobile` in Android Studio Hedgehog+.
3. Run on emulator (API 34). `BuildConfig.API_BASE_URL` = `http://10.0.2.2:5205` (emulator → host).
4. Login with `200012345678 / 123456` (Prosumer) or `199512345678 / 123456` (Operator).

## What works offline
Live `GET /api/solarstations` first, fallback to `MockData` + SQLite `StationCache`.
Map tiles need internet; offline shows the schematic canvas. QR scan is stubbed;
`QrDemoHelper` generates demo codes only.

## Ownership
Each member owns their `app/<feature>/` folder. Never edit `Frontend/*` or other
members' backend files. Backend routes used: `/api/solarstations`, `/api/health`.

## Demo (30s)
Splash → Login (demo button) → Map (search/filter/stats) → Full details → Book stub toast.
