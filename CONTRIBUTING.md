# Contributing — Smart Solar Microgrid Trading System

Team rule #1: **create, don't invade.** Every member owns a folder (frontend)
and a file set (backend). You only ever add your own files — never edit
another member's.

## Folder layout

```text
Smart-Solar-Microgrid-Trading-System/
├── SmartSolarMicrogridAPI/          # ONE shared backend (see file ownership below)
│   ├── Controllers/
│   ├── Models/
│   ├── Services/
│   └── .env.example                 # shared variable names — copy to .env, never commit .env
├── SmartSolarStationUI/            # Member 2 — solar stations / infrastructure ✅
├── SmartSolarUsersUI/              # Member 1 — dashboard, users, profile, auth
├── SmartSolarReservationsUI/       # Member 3 — energy booking flow
├── SmartSolarFieldOpsUI/           # Member 4 — QR verification + full maps
└── CONTRIBUTING.md                 # this file
```

New UI folders copy the `SmartSolarStationUI` skeleton
(`index.html` + `css/` + `js/` + `assets/`) and live as flat siblings at the
repo root. Do NOT nest them under a new `frontend/` folder and do NOT move
existing folders — moves break paths and PRs for zero benefit.

## Ownership

| Area | Member 1 | Member 2 | Member 3 | Member 4 |
|---|---|---|---|---|
| UI folder | `SmartSolarUsersUI/` | `SmartSolarStationUI/` ✅ | `SmartSolarReservationsUI/` | `SmartSolarFieldOpsUI/` |
| API controllers | `UsersController` | `SolarStationsController`, `HealthController` ✅ | `ReservationsController` | `QrController` |
| API models/services | `User*` | `SolarStation*` ✅ | `Reservation*` | `Qr*` |
| Mongo collection | `Users` | `SolarStationInfo` ✅ | `EnergyReservation` | own |
| API route prefix | `/api/users` | `/api/solarstations`, `/api/health` ✅ | `/api/reservations` | `/api/qr` |

## Backend rules (shared project!)

- Add **only** your own controllers / models / services with your route prefix
  and your collection. Never edit another member's files.
- `Program.cs` is shared: **additive registrations only**
  (e.g. one `AddSingleton<ReservationService>()` line). Announce it in the
  group chat before pushing.
- One database: **`SmartSolarDB`**. Different collections per module.
- Config: copy `SmartSolarMicrogridAPI/.env.example` → `.env`, paste the
  shared Atlas URI. **Reuse the exact variable names**
  (`MONGODB_CONNECTION_STRING`, `MONGODB_DATABASE`, `MONGODB_COLLECTION`) —
  do not invent new ones. Never commit `.env`.

## Frontend rules

- Your module = your folder. It must run **standalone**: Live Server (or
  `python3 -m http.server`) on its own port, talking to the one shared API
  (`http://localhost:5205` — keep this in a single `config.js` like Member 2).
- Cross-member UI edits are limited to **one thing**: swapping a sidebar
  `data-module` placeholder toast for your real page link when you deliver.
  Each sidebar item documents its owner — check before touching.
- Don't duplicate shared assets without reason; link, don't fork.

## Git workflow

```powershell
git clone <repo-url>
git fetch origin
git checkout -b feature/<your-module>   # never commit on main
# stay current:
git merge origin/main
# submit:
git push origin feature/<your-module>   # then open a PR — never force-push
```

Before pushing: `git status` must show only your files, and never `.env`.

## Definition of done (per module)

- [ ] API endpoints live + tested (Swagger/Postman proofs)
- [ ] UI page runs standalone against the shared API
- [ ] Data visible in Compass under the agreed collection
- [ ] No files outside your ownership touched (check with `git status`)
- [ ] PR to `main` with a short demo script

## Message for the group chat (copy-paste)

> Team rules are now in CONTRIBUTING.md — please read before coding:
> (1) each member gets their own UI folder (`SmartSolar<Module>UI`, copy the
> Member 2 skeleton) and their own backend files — never edit another
> member's files; (2) one shared DB `SmartSolarDB`, separate collections,
> separate `/api/*` prefixes; (3) Program.cs edits are additive-only, announce
> first; (4) copy `.env.example` → `.env` for secrets, never commit `.env`;
> (5) sidebar placeholders link up when your page is ready. Feature branches +
> PRs, no direct pushes to main.
