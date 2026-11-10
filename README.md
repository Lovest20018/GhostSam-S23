# GhostSam S23

**English** | [中文](README.zh.md)

A Samsung Galaxy S23 temporary root app, adapted from
[snothin/GhostSam](https://github.com/snothin/GhostSam) for the China-market
**SM-S9110**. This fork uses DirtyFrag standalone
and official **KernelSU Manager 3.3.1**, with matching UAPI 5 components.
GhostSam S23 maintains its own product versions and release schedule. Upstream
changes are reviewed and adopted selectively.

[Download the verified build 65 test APK (Actions artifact)](https://github.com/Lovest20018/GhostSam/actions/runs/42744246916)
· [S23 build details (中文)](docs/KernelSU-3.3.1.zh.md)
· [Home states and upstream maintenance (中文)](docs/Home-copy-and-upstream.zh.md)
· [Independent release policy (中文)](docs/S23-product-and-releases.zh.md)

## Supported target

| Item | Required value |
| --- | --- |
| Device | Galaxy S23, China-market SM-S9110 |
| Firmware and build | No exact match in current development source |
| System | Android 16 or later / SDK ≥ 36 |
| Kernel release | No exact match; kernel-window and runtime checks remain |
| KMI | android13-5.15 |
| Manager | Official KernelSU Manager 3.3.1 |

Current development source checks the model, minimum SDK, KMI, and runtime
prerequisites. Firmware identifiers such as FZI1 or FZJ1 and exact kernel
release strings no longer determine acceptance. Build 65 includes these checks;
the published r3 APK retains the old exact match. On 2026-11-10, the user reported root
and home-state recovery after reopening working on SM-S9110, One UI 8.5 /
Android 16, firmware `S9110ZCS8FZJ1`.
Later systems such as One UI 9 are not rejected by version alone, but root
availability depends on the actual kernel, patches, and matching modules
and remains unverified on One UI 9. This APK bundles only android13-5.15
modules; another KMI requires module adaptation.

## Using the S23 app

1. Download `ghostsam-s23-apk` from the verified Actions run linked above,
   extract `GhostSam-S23.apk`, and install it with official **KernelSU Manager 3.3.1**.
   The S23 package is `com.snothin.ghostsam.s23` and can coexist with upstream GhostSam.
2. On first use or after a full reboot, open GhostSam S23 and run the temporary
   root flow. This edition uses standalone mode and official KernelSU.
3. If KSU already shows **Jailbroken**, open KSU and grant **GhostSam S23**
   su access under **Superuser**, then return to the app. A denied su check
   alone does not mean KernelSU needs to be loaded again.
4. After the app verifies su access, its home card shows **KernelSU enabled**.
   The app rechecks authorization when reopened or brought to the foreground,
   and retries failed checks. Open KSU from the card to use the manager's
   soft reboot action.

Build 65 and build 64 use different CI debug signing keys. To replace build 64,
uninstall the old test app, install build 65, and grant GhostSam S23 su access
again in KSU. The phone can keep its existing root; this installation does
not require running the root flow again.

On the user's target phone, granting GhostSam su access and then opening KSU
from GhostSam allowed the manager's soft reboot action to work. Temporary
root lasts until a full reboot; a soft reboot does not replace an already
loaded driver when testing an updated build.

## Source and APK versions

| Version | Status |
| --- | --- |
| `0.3.0-dev` (build 65) | Current `main`; fixes the home status resetting after reopening, retries local su independently, and stores module loading for the current kernel boot; [complete CI passed and APK available](https://github.com/Lovest20018/GhostSam/actions/runs/42744246916); user reported root and reopening working on FZJ1; not a published release |
| build 64 | Merged upstream v0.2.4 and matching KernelSU changes, with firmware and exact kernel matching removed; [complete CI test build passed](https://github.com/Lovest20018/GhostSam/actions/runs/42739046856) |
| r3 (`0.2.3-s23-uapi5-r3`, build 63) | [Published test APK](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3); includes the revised home states and authorization hints; CI build and checks passed |
| r2 | Previously published test APK; see the release notes for installation details |

The verified build 65 APK comes from
[`7fe1fdc`](https://github.com/Lovest20018/GhostSam/commit/7fe1fdcbeb306f7f67bf2b0fc46bf7003ffea847).
Its CI passed unit tests, driver and ksud compilation, APK packaging, payload
checks, and signature verification. APK SHA-256:
`b3b3a468ed1c92e82bffabad6d6b82c5a45ba06b24570f012733b35a1efa94f4`.
The latest published Release remains
[r3](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3),
which retains the older firmware restrictions. The current user feedback
covers root and reopening on the reported FZJ1 phone; it does not establish
One UI 9 or full reboot coverage. See the [validation record (中文)](docs/Home-copy-and-upstream.zh.md#重新打开后的状态恢复build-65).

## Building

For a complete build, open [Actions](https://github.com/Lovest20018/GhostSam/actions/workflows/kernelsu-331.yml),
select **Build GhostSam S23**, choose `main`, and use **Run workflow**.
The workflow rebuilds the Samsung KernelSU driver and ksud from the pinned
source, then builds and verifies the S23 APK. Download the
`ghostsam-s23-apk` artifact from a successful run.

Pushes to `main`, pull requests, and manual runs build test artifacts without
publishing a release. S23 is the default Gradle build. Its version comes from
[`app/s23-version.properties`](app/s23-version.properties), independently of
the legacy upstream version file, and builds do not increment it.

New releases use `s23-v<versionName>` tags, for example `s23-v0.3.0-rc.1`.
Publication requires an exact version/tag match, a build number above the
latest published APK, release notes, and a successful complete build. All
current releases are prereleases; `-dev` versions produce artifacts only.
The existing r2/r3 releases remain available. See the
[release policy](docs/S23-product-and-releases.zh.md).

Local builds require JDK 21, SDK 37.0, NDK 30.0.16248370, CMake 3.22.1, and
the matching rebuilt UAPI 5 payloads. Follow the
[build guide](docs/KernelSU-3.3.1.zh.md) to prepare the payloads first, then run:

```bash
cd app
./gradlew :app:testDebugUnitTest :app:assembleDebug -Ps23Only=true --no-daemon --no-configuration-cache
```

The S23 build packages only ksud and the android13-5.15 installation module.
It excludes other KMIs, KernelSU-Next, and the companion APK.

## Upstream maintenance

`main` is the independently maintained S23 product branch. Its integrated
upstream source remains [snothin/GhostSam](https://github.com/snothin/GhostSam), based on
v0.2.4 (`8fa2f68`). The original toolkit and its device coverage are described
in the [upstream README](https://github.com/snothin/GhostSam/blob/8fa2f68c39119eeea46f059057fecc5f371ee427/README.md).
S23 home states and localized copy live in separate files. The 2026-11-10 merge
preserves the S23 package, independent version, UAPI 5 checks, and authorization
states. See the [integration record (中文)](docs/upstream/2026-11-10-v0.2.4-merge.zh.md).

Check upstream changes from a local checkout with the configured `upstream`
remote:

```bash
python scripts/check-upstream.py --fetch
```

The command distinguishes the integrated baseline, the last reviewed commit,
and new commits awaiting review. Review does not imply adoption. Apply selected
changes on a temporary branch and record their source and validation before
integrating into `main`. See the [maintenance guide](docs/Home-copy-and-upstream.zh.md).
KernelSU is pinned separately to `snothin/KernelSU` commit
`b086147e79e2725177d960e7161ba279288efc9d`; updating GhostSam does not change
that driver baseline automatically.

## Credits and license

- [snothin/GhostSam](https://github.com/snothin/GhostSam): the original app,
  shared infrastructure, and Ghostlock / DirtyFrag integration.
- [snothin/KernelSU](https://github.com/snothin/KernelSU) and
  [KernelSU](https://github.com/tiann/KernelSU): the Samsung driver, ksud,
  and manager work. The upstream tree also includes
  [KernelSU-Next](https://github.com/snothin/KernelSU-Next) integration.
- [ghostlock-s26](https://github.com/snothin/ghostlock-s26), @diabl0w's
  Z Fold8 porting input, and [DFRoot](https://github.com/diabl0w/DFRoot):
  upstream exploit work and public DirtyFrag research.

For security research on devices you own and control. Kernel exploitation
can cause crashes or reboots; compatibility must be reviewed after a system
update. Licensed under [Apache-2.0](LICENSE). Third-party provenance and
acknowledgements are preserved in [NOTICE](NOTICE).
