# GhostSam S23

**English** | [中文](README.zh.md)

A Samsung Galaxy S23 temporary root app, adapted from
[snothin/GhostSam](https://github.com/snothin/GhostSam) for the China-market
**SM-S9110**. This fork uses DirtyFrag standalone
and official **KernelSU Manager 3.3.1**, with matching UAPI 5 components.
GhostSam S23 maintains its own product versions and release schedule. Upstream
changes are reviewed and adopted selectively.

[Download the latest r3 test APK](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3)
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
release strings no longer determine acceptance. The published r3 APK still
uses the old exact match; a rebuilt APK is required for this change.
Later systems such as One UI 9 are not rejected by version alone, but root
availability depends on the actual kernel, patches, and matching modules
and has not been verified on a phone. This APK bundles only android13-5.15
modules; another KMI requires module adaptation.

## Using the S23 app

1. Install **GhostSam S23** and official **KernelSU Manager 3.3.1**. The S23
   package is `com.snothin.ghostsam.s23` and can coexist with upstream GhostSam.
2. On first use or after a full reboot, open GhostSam S23 and run the temporary
   root flow. This edition uses standalone mode and official KernelSU.
3. If KSU already shows **Jailbroken**, open KSU and grant **GhostSam S23**
   su access under **Superuser**, then return to the app. A denied su check
   alone does not mean KernelSU needs to be loaded again.
4. After the app verifies su access, its home card shows **KernelSU enabled**.
   Open KSU from that card to use the manager's soft reboot action.

On the user's target phone, granting GhostSam su access and then opening KSU
from GhostSam allowed the manager's soft reboot action to work. Temporary
root lasts until a full reboot; a soft reboot does not replace an already
loaded driver when testing an updated build.

## Source and APK versions

| Version | Status |
| --- | --- |
| r2 | Previously published test APK; see the release notes for installation details |
| r3 (`0.2.3-s23-uapi5-r3`, build 63) | [Published test APK](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3); includes the revised home states and authorization hints; CI build and checks passed |
| `0.3.0-dev` (build 64) | Merged upstream v0.2.4 and matching KernelSU changes, with firmware and exact kernel matching removed; [complete CI test build passed](https://github.com/Lovest20018/GhostSam/actions/runs/42739046856); not a published release |

The r3 home card distinguishes checking, manager missing, ready, running,
loaded but awaiting su access, and enabled. These changes require a new APK;
install the r3 APK from the release assets to update an installed r2 app.
The package uses this CI run's debug signing key; see the release notes if
Android reports a signature mismatch. The r3 APK has not been tested on the phone yet.

Build source: [`14f1343`](https://github.com/Lovest20018/GhostSam/commit/14f1343521dba0b3e03d1814daf4ae8d7a065b8d)
· [Successful Actions run](https://github.com/Lovest20018/GhostSam/actions/runs/38679062368).

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
`abbd7be7fb5e0af607c2a85e02efae03044bba86`; updating GhostSam does not change
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
