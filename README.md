# MUreset

Desktop automation for MU Online (Windows) built with **Compose Desktop**. The app manages character profiles and runs a full reset workflow when a character hits the target level, distributing points based on your configuration.

## Quick Summary
- **UI**: character profiles, attributes, solo level, warp map, logs.
- **Bot**: finds game windows, reads stats, runs `/reset`, distributes points, warps, toggles hunt, and re-joins party.
- **Persistence**: saves character list and bot settings in the project root.
- **Web Dashboard (LAN)**: status, logs, characters, stats, and screenshots.

## Requirements
- **Windows** (Win32 APIs via JNA)
- **JDK 17** (Gradle toolchain 17)

## Run (dev)
```bash
./gradlew run
```

## Build (Windows executable)
```bash
./gradlew package
```
Typical output:
- `build/compose/binaries/main/app/MUreset/MUreset.exe`

## How to Use
1. Open the game with the desired characters.
2. In the app, click **Add character** and fill:
   - **Name**: must match the character name in the window title.
   - **Attributes** (Str/Agi/Sta/Ene/Cmd).
   - **Points/Reset**: points per reset on your server.
   - **Solo level**: target level after reset.
   - **Warp map**: Elbeland 2 or Elbeland 3.
   - **Overflow attribute**: attribute that receives remaining points.
3. Click **Start**.

## Bot Flow
- Finds windows by title prefix:
  - `GlobalMuOnline - Powered by IGCN - Name: [NAME]`
- Parses **Level**, **Master Level**, **Resets** from the title.
- When `Level == 400`:
  - executes `/reset`
  - distributes points via `/addstr`, `/addagi`, `/addvit`, `/addene`, `/addcmd`
  - caps overflow at **32,600** (logs remaining points if exceeded)
- After reset:
  1. Warp to the configured map.
  2. Toggle hunt with **Home** (with validation retries).
  3. Wait until **Solo level** is reached.
  4. Re-join party and validate exit from Elbeland (retries if needed).
- **Idle guard**: waits for 30s of user inactivity before running.

## Web Dashboard (LAN)
- Server runs on: `http://<LAN-IP>:8765`
- Shows:
  - bot status + active character
  - last logs
  - characters list
  - per-character stats (level/master/resets)
  - latest screenshot per active character

## Saved Config Files (USER_HOME)
- `~/.mureset-characters.cfg`
- `~/.mureset-settings.cfg`

## Visual Templates (OpenCV)
- `src/main/resources/play_button_template.png`
- `src/main/resources/pause_button_template.png`
- `src/main/resources/ok_dialog_template.png`
- `src/main/resources/elbeland2_template.png`
- `src/main/resources/elbeland3_template.png`
- `src/main/resources/current_map_elbeland.png`
- `src/main/resources/quest_dialog_template.png`

## Notes
- The bot uses `Robot` for mouse/keyboard. Avoid using the PC while it runs.
- If the game window title changes, update `BotController.windowPrefix`.
