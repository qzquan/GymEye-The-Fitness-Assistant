# GymEye Backend

## Setup

1. Run `npm install` if dependencies are missing.
2. Start the server with `npm start`.

The backend now uses a local JSON data file at `backend/data/db.json` by default. On first start it auto-creates the file and imports equipment names from the Android label asset.

## Android development startup

The Android debug build starts the local backend automatically. Android Studio Run, `gradlew installDebug`, and `gradlew assembleDebug` trigger `:app:ensureBackendRunning`, which starts `node backend/src/index.js` when port `8080` is not already listening.

If Android Studio cannot find Node.js, the Android build now continues and prints a warning. Install Node.js, set `GYMEYE_NODE` to the full `node.exe` path, or pass `-PnodeExecutable=C:\path\to\node.exe`. Pass `-PrequireBackendAutostart=true` if you want missing backend startup to fail the build.

To skip this behavior for a build:

```powershell
.\gradlew.bat assembleDebug -PskipBackendAutostart=true
```

From the repo root, `run-dev.cmd` starts the backend, installs the debug app, and launches it on the connected emulator/device.

## Available scripts

- `npm start`: start the backend on `PORT`
- `npm run start:dev`: start with Node watch mode
- `npm run smoke`: run a basic API smoke test against the running server

Legacy MySQL scripts are still present in the repo, but the active runtime path no longer depends on MySQL.

## API summary

### Health

- `GET /api/health`

### User

- `POST /api/user/register`
- `POST /api/user/login`
- `GET /api/user/me`
- `PATCH /api/user/me`

`/api/user/login` keeps the old behavior: if the email does not exist, it auto-registers and returns `mode: "registered"`.

### Equipment

- `GET /api/equipment`
- `GET /api/equipment/targets`
- `GET /api/equipment/id/:id`
- `GET /api/equipment/:name`
- `POST /api/equipment`
- `PATCH /api/equipment/:id`
- `DELETE /api/equipment/:id`

### History

- `POST /api/history`
- `POST /api/history/add`
- `GET /api/history`
- `GET /api/history/list`
- `GET /api/history/stats/summary`
- `DELETE /api/history/:id`

History endpoints accept JWT via `Authorization: Bearer <token>`. For compatibility, `POST /api/history/add` and `GET /api/history/list` still accept `userId`.

## Notes

- Equipment write endpoints require JWT auth.
- History delete and summary endpoints require JWT auth.
- Current health response reports `storage.driver = "file"` when the file store is active.
