# Screenshots

The README embeds eight images from this folder. Drop your device captures here
using **exactly** these filenames, or the images will render as broken links:

| Filename | Screen |
|---|---|
| `dashboard.png` | Dashboard with the stat cards |
| `live_camera.png` | Live view with a face box |
| `profiles.png` | People list |
| `alerts.png` | Alerts gallery |
| `scan_photo.png` | Enrolling from a gallery photo |
| `add_profile.png` | Add Person with the aligned crop preview |
| `weapon_detect.png` | Live view with an orange weapon box |
| `settings.png` | Settings |

## Taking them

```
adb exec-out screencap -p > dashboard.png
```

Or use the phone's own screenshot gesture and copy the files across.

## Before you commit

These go into a **public** repository. Check each image for anything you would
not publish:

- **Faces of real people.** The People screen and any live capture will show
  them. Use your own face, or a printed photo of a stock portrait.
- **Real names** on enrolled profiles.
- The **status bar** — carrier, and any notification previews.

PNG at phone resolution is typically 200–600 KB each, which is fine to commit.
If a capture runs to several megabytes, scale it down: the README renders them
at 180 px wide, so nothing larger than about 720 px is doing any work.
