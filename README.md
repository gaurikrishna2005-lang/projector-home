# Projector Home

A tiny, dark home screen for the HY300 Pro projector (Android 11, 1 GB RAM).

- Clock and date
- Four big tiles: **Netflix, YouTube, Prime Video, HDMI**
- Small buttons: **Picture** (keystone), **Wallpaper**, **All apps**, **Settings**
- Your own photo as the background: open **Wallpaper**, then visit the address it shows
  from your phone's browser (same Wi-Fi) and pick photos. The upload page only runs while
  the Wallpaper screen is open.
- No animations, no background services

## Download

Every change to this repo builds the app automatically (see the **Actions** tab).
The latest build is always at:

```
https://github.com/gaurikrishna2005-lang/projector-home/releases/latest/download/projector-home.apk
```

## Remote

| Button | What it does |
|---|---|
| Arrows + OK | Move and open |
| Home | Back to the first tile |
| Menu | All apps |
| Hold OK on an app in *All apps* | App info (disable / uninstall) |

## Changing the tiles

Edit the `tiles` list at the top of
`app/src/main/java/home/projector/HomeActivity.java`. Each tile finds its app by package
name, and falls back to searching app names for the second word given.

## PC scripts (Windows)

In `scripts/`. Copy them into the `platform-tools` folder and double-click.

- `1-cleanup.bat` switches off the suspected proxy apps, turns off animations and saves `projector-info.txt`
- `2-install-home.bat` installs the app (put `projector-home.apk` next to it) and makes it the home screen
- `undo-cleanup.bat` puts everything back as it came

## Signing key

`app/release.keystore` (password `projectorhome`) is committed on purpose so every build
installs over the last one. It's only used for this personal app.
