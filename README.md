<!-- geostamp-readme-v4 -->
# GeoStamp

**Version 1.0.0** - an Android camera and photo tool that stamps a place name, address, coordinates, date and time onto pictures.
It runs on the phone: no account, no ads, no server of its own.

<!-- Add screenshots here, for example docs/screenshots/camera.png -->

## Features
- **Camera:** live preview (CameraX), flash, front/back camera. A one-time location lookup happens when the photo is taken.
- **Stamp editor:** live preview; place name, address, latitude, longitude, label, date, time and time zone are all editable. Style: text size, padding, colours, date and time format.
- **Two layouts:** a classic bar across the bottom, or a **map card** with a small map picture, a title, the address, coordinates, a long date and a note.
- **Saved places:** keep places you use often. Type a name, a town or a group code in the editor and pick a suggestion; its name and coordinates are filled in and the name is written before the address. Manage places in Settings.
- **Google Maps:** share a place from Maps to GeoStamp, or paste a link or coordinates.
- **Gallery:** albums (real folders), full-screen viewer with zoom and left/right swiping, details, rename, delete, share, export to a folder.
- **Many photos at once:** import many photos, select several in the gallery and stamp, share, export, move or delete them together.
- **Metadata (EXIF):** inspect what is inside a photo; optionally write changes into a new copy. The stamp drawn on the picture and the hidden metadata are different things.

Saving always makes a new copy in `Pictures/GeoStamp`. The original photo stays untouched inside the app.
The stamp does not say whether a location came from GPS or was typed; the app records that (editor and photo details). A stamp with a typed location is a label, not proof that a photo was taken there.

## Requirements
- Android Studio (current stable) with its bundled JDK and the Android SDK
- A phone or emulator with **Android 10 (API 29) or newer**
- Internet on the first build to download Gradle and libraries

## Build and run
1. Open the project folder in Android Studio and wait for the Gradle sync.
2. Press the green Run button.
3. Command line: `./gradlew assembleDebug` builds the debug app; `./gradlew testDebugUnitTest` runs the unit tests.
4. On a phone or emulator, run `StampRendererInstrumentedTest` from `app/src/androidTest`.

GitHub Actions (`.github/workflows/android.yml`) runs the unit tests and builds the debug app on every push.

## Permissions and the internet
| Permission | Why |
| --- | --- |
| Camera | Taking photos |
| Location (precise or approximate) | Writing coordinates and an address on a stamp when a photo is taken; optional |
| Internet | Only to read a Google Maps short link you paste or share, to download the small OpenStreetMap picture for the map card, and for Android's address lookup |
| none for storage | Photos are saved with MediaStore and picked with the system photo picker |

See [PRIVACY.md](PRIVACY.md). The map card uses OpenStreetMap tiles (c) OpenStreetMap contributors.

## Optional: a starting list of places
The app contains no places. To bundle your own list into a build, copy `docs/seed_places.example.json` to
`app/src/main/assets/seed_places.json` and edit it. That file is ignored by Git, so a private list never reaches the repository.
It is read once, the first time the app runs.

## How it is built
Kotlin, Jetpack Compose (Material 3, own theme and Material icons), CameraX, AndroidX ExifInterface, coroutines. No dependency-injection or database library.

```
domain/   pure Kotlin rules with unit tests: stamp text, card layout, editing, map links, tile maths, place search, batch rules, EXIF helpers
data/     storage and Android services: photo and place stores (JSON files), MediaStore saving, bitmap loading, location, camera, map tiles, EXIF
ui/       theme/ and design/ (the shared look), then the screens and view models
app/      AppServices: creates the shared helpers
```
Photos live in three places: the private original (never modified), the stamped copy (`Pictures/GeoStamp/...`, visible in other apps) and a small record in `photos.json` (stamp text and style, so a stamp can be edited again).

## Known limits
- Android 10+ only. Saved copies are limited to 4096 pixels on the long edge.
- Moving, renaming and deleting only work on files that this install created.
- The address comes from Android's address service and normally needs internet; offline you can type it.
- The map picture needs internet; without it the card is saved without a map and the app says so.
- There is no live map inside the app. Use Google Maps and share the place, or use a saved place.

## Contributing and licence
Issues and pull requests are welcome. Licensed under the [MIT License](LICENSE). Changes are listed in [CHANGELOG.md](CHANGELOG.md).
