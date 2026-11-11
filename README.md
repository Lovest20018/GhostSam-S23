# GhostSam S23

**English** | [中文](README.zh.md)

<img src="docs/assets/ghostsam-s23-icon.svg" width="64" height="64" alt="GhostSam S23 icon">

Temporary root for the China-market **Samsung Galaxy S23 (SM-S9110)**,
using DirtyFrag standalone and **KernelSU Manager 3.3.1 / UAPI 5**.

[Download 0.3.1 APK](https://github.com/Lovest20018/GhostSam-S23/releases/download/s23-v0.3.1/GhostSam-S23.apk)
· [Release notes](https://github.com/Lovest20018/GhostSam-S23/releases/tag/s23-v0.3.1)

## Compatibility

- **Device:** SM-S9110; Android 16 or later; KMI `android13-5.15`.
- Firmware identifiers and full kernel strings are not locked; runtime compatibility checks remain.
- User-reported working: One UI 8.5 / Android 16 / `S9110ZCS8FZJ1`, including root and reopening the app (build 65).
- One UI 9 and other KMIs have not been validated.

## Usage

1. Install GhostSam S23 and official KernelSU Manager 3.3.1.
2. On first use or after a full reboot, run the temporary root flow in GhostSam S23.
3. If KSU already shows **Jailbroken**, grant **GhostSam S23** su access under **Superuser**, then return. The app verifies access and shows **KernelSU enabled**.

The home card opens KSU, where you can use soft reboot. Temporary root ends
on a full reboot. Reopening the app rechecks authorization and retries failed checks.

CI signing keys differ between builds. If Android reports a signature conflict,
save any settings or logs you need, uninstall the old GhostSam S23, install the
new APK, and grant su access again. Existing root can remain active.

## What's new in 0.3.1

- Fixes release retries and older drafts replacing a newer GitHub Latest release.
- Adds publication regression tests and serializes release publication.
- Keeps 0.3.0's root status recovery, firmware compatibility policy, and redesigned ghost icon.

## Development and documentation

[Build guide](docs/KernelSU-3.3.1.zh.md) · [Validation and upstream maintenance](docs/Home-copy-and-upstream.zh.md) · [Release policy](docs/S23-product-and-releases.zh.md)

Build with [Actions → Build GhostSam S23](https://github.com/Lovest20018/GhostSam-S23/actions/workflows/kernelsu-331.yml).
Versions are maintained independently in `app/s23-version.properties`.
Release downloads include `BUILD-INFO.txt` and `SHA256SUMS`.

Maintained by [Lovest20018](https://github.com/Lovest20018). Based on [snothin/GhostSam](https://github.com/snothin/GhostSam) and
[Samsung KernelSU](https://github.com/snothin/KernelSU).
For devices you own and control. [Apache-2.0](LICENSE); full credits in [NOTICE](NOTICE).
