<div align="center">

# M3U Player

**A simple M3U player for Android phones and tablets.**

[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/github/license/mirko77/M3UAndroid-player)](LICENSE)

</div>

---

M3U Player plays IPTV streams from your own M3U playlists (plus XMLTV programme guides).

It started as a fork of [M3UAndroid by oxyroid](https://github.com/oxyroid/M3UAndroid),
which is archived and no longer maintained. The goal of this project is to keep
maintaining a simple M3U player for phones and tablets.
The fork keeps the phone/tablet app and deliberately simplifies it:

- M3U playlists (and their EPG guides) only. Xtream Codes, Emby, Jellyfin,
  generic providers, and external extensions were removed.
- No Android TV app, no Play Store release. Sideload the APK from
  [GitHub Releases](https://github.com/mirko77/M3UAndroid-player/releases/latest).
- Android 10+ only.
- Extras removed: debug playback samples, God Mode, unseen-channel
  recommendations, TV remote control, extension plugins UI, standalone
  programme-guide (EPG) source management, and hidden-category management.
- Extras added: open-on-favorites startup tab, auto-landscape player,
  compact headline cards.

## Download

- [GitHub Release](https://github.com/mirko77/M3UAndroid-player/releases/latest)

## Build

```sh
git submodule update --init --recursive
./gradlew :app:smartphone:assembleDebug
```

Requirements: JDK 17 (the `parser` submodule pins `jvmToolchain(17)`),
Android Studio Otter 2.x or newer (project pins AGP 9.0.0 / compileSdk 36),
Android SDK platform 36.

## License

M3U Player is an open-source project licensed under the [GNU General Public License v3.0](LICENSE).
