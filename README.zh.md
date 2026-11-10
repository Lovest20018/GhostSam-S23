# GhostSam S23

**中文** | [English](README.md)

面向国行 **三星 Galaxy S23（SM-S9110）** 的临时 root
应用，基于 [snothin/GhostSam](https://github.com/snothin/GhostSam) 适配。
本 fork 固定使用 DirtyFrag standalone 路径与官方 **KernelSU Manager 3.3.1**，
配套构建 UAPI 5 驱动和 ksud。
GhostSam S23 独立维护产品版本和发布节奏，上游变化经过审查后按需采用。

[下载最新 r3 测试版 APK](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3)
· [S23 构建与兼容说明](docs/KernelSU-3.3.1.zh.md)
· [应用首页与上游维护说明](docs/Home-copy-and-upstream.zh.md)
· [独立产品与发布规则](docs/S23-product-and-releases.zh.md)

## 支持范围

| 项目 | 要求 |
| --- | --- |
| 机型 | 国行 Galaxy S23，SM-S9110 |
| 固件与编译编号 | 当前开发源码不再精确匹配 |
| 系统 | Android 16 及后续版本 / SDK ≥ 36 |
| 完整内核版本 | 不再精确匹配；保留内核窗口和运行前探测 |
| KMI | android13-5.15 |
| 管理器 | 官方 KernelSU Manager 3.3.1 |

当前开发源码按机型、最低 SDK、KMI 和运行前探测检查兼容性，
不因 `FZI1`、`FZJ1` 等固件编号或完整内核字符串变化而直接拒绝。
已发布 r3 仍保留旧版精确匹配，源码修改需要重新构建 APK 才能生效。
One UI 9 等后续系统不会因系统版本编号被拒绝，但能否获取 root 取决于
实际内核、补丁和配套模块，尚未完成真机验证。当前 APK 仅包含
`android13-5.15` 模块；若 KMI 改变，需要重新适配模块。

## 使用方法

1. 安装 **GhostSam S23** 和官方 **KernelSU Manager 3.3.1**。
   S23 版包名为 `com.snothin.ghostsam.s23`，可与原版 GhostSam 同时安装。
2. 首次使用或完整重启后，打开 GhostSam S23，执行获取临时 root。
   S23 版固定使用 standalone 和官方 KernelSU。
3. 如果 KSU 已显示 **“已越狱”**，先进入 KSU 的 **“超级用户”**，
   授予 **GhostSam S23** su 权限，再返回应用。单次 su 检测失败不能说明需要重新加载。
4. 应用验证 su 可用后，首页显示 **“KernelSU 已启用”**。
   从该卡片打开 KSU，再使用管理器中的软重启。

用户已在上述目标设备上反馈：授予 GhostSam su 权限后，从 GhostSam 打开 KSU，
软重启可以正常使用。临时 root 在完整重启后需要重新获取；测试新版驱动时，
软重启不能替代完整重启来切换已加载的驱动。

## 源码与安装包状态

| 版本 | 状态 |
| --- | --- |
| r2 | 已发布的测试版 APK，安装注意事项见对应 Release |
| r3（`0.2.3-s23-uapi5-r3`，build 63） | [已发布测试版 APK](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3)，包含新的首页状态和授权提示；CI 构建与检查已通过 |
| `0.3.0-dev`（build 65） | 当前开发源码：修复重新打开后回到初始状态，独立检测本地 su，并按本次开机保存模块加载记录；尚未发布 Release |
| build 64 | 合并上游 v0.2.4，配套升级 KernelSU，移除固件与完整内核精确匹配；[完整 CI 测试构建已通过](https://github.com/Lovest20018/GhostSam/actions/runs/42739046856) |

r3 首页区分正在检测、管理器未安装、准备执行、执行中、已加载待授权、已启用。
请下载 Release 附件中的 r3 APK 更新手机上的 r2。
本版使用本次 CI 的 debug 签名，若提示签名冲突，按 Release 安装说明处理。
r3 APK 尚未进行真机验证。

构建源码：[`14f1343`](https://github.com/Lovest20018/GhostSam/commit/14f1343521dba0b3e03d1814daf4ae8d7a065b8d)
· [已通过的 Actions 构建](https://github.com/Lovest20018/GhostSam/actions/runs/38679062368)。

## 构建

完整构建可进入 [Actions](https://github.com/Lovest20018/GhostSam/actions/workflows/kernelsu-331.yml)，
选择 **Build GhostSam S23**，在 **Run workflow** 中选择 `main` 手动运行。
工作流从固定源码重建三星 KernelSU 驱动与 ksud，再构建、校验 S23 APK。
成功后下载该次运行的 `ghostsam-s23-apk` artifact。

推送 `main`、提交 PR 或手动运行会构建测试 artifact，不自动发布 Release。
Gradle 默认构建 S23，版本单独读取 [`app/s23-version.properties`](app/s23-version.properties)，
构建不自动递增版本号，也不读取旧上游版本号。

新发布标签为 `s23-v<versionName>`，例如 `s23-v0.3.0-rc.1`。标签必须与
版本文件一致，build 必须高于已发布 APK，提供对应发布说明，并通过完整构建和校验。
现阶段统一发布测试版；`-dev` 版本只能生成 artifact。既有 r2/r3 Release 继续保留，
具体操作见[发布规则](docs/S23-product-and-releases.zh.md)。

本地构建需要 JDK 21、SDK 37.0、NDK 30.0.16248370、CMake 3.22.1，
以及同源重建的 UAPI 5 payload。先按[构建说明](docs/KernelSU-3.3.1.zh.md)
准备 payload，再执行：

```bash
cd app
./gradlew :app:testDebugUnitTest :app:assembleDebug -Ps23Only=true --no-daemon --no-configuration-cache
```

S23 版只打包 ksud 与 android13-5.15 安装辅助模块，排除其他 KMI、
KernelSU-Next 和 companion APK。

## 与上游的关系

**`main` 是独立维护的 S23 产品主线**。原项目为
[snothin/GhostSam](https://github.com/snothin/GhostSam)，当前基于 v0.2.4
（`8fa2f68`）。上游完整工具包和机型列表见
[上游原版说明](https://github.com/snothin/GhostSam/blob/8fa2f68c39119eeea46f059057fecc5f371ee427/README.zh.md)。
S23 首页状态与中英文文案放在独立文件中。2026-11-10 合并 v0.2.4，
保留 S23 的包名、独立版本、UAPI 5 校验及授权状态；详见
[合并记录](docs/upstream/2026-11-10-v0.2.4-merge.zh.md)。

在已配置 `upstream` 远程的本地仓库中检查上游变化：

```bash
python scripts/check-upstream.py --fetch
```

检查器分别展示已整合基线、已审查提交和新增待审查提交；审查不代表已经采用。
后续在临时分支移植需要的改动，记录来源与验证结果后再整合到 `main`，
具体步骤见[维护说明](docs/Home-copy-and-upstream.zh.md)。
KernelSU 单独固定在 `snothin/KernelSU` 的
`abbd7be7fb5e0af607c2a85e02efae03044bba86`；同步 GhostSam 不会自动升级驱动基线。

## 致谢与许可

- [snothin/GhostSam](https://github.com/snothin/GhostSam)：原始应用、共享基础设施，
  以及 Ghostlock / DirtyFrag 集成。
- [snothin/KernelSU](https://github.com/snothin/KernelSU) 与
  [KernelSU](https://github.com/tiann/KernelSU)：三星驱动、ksud 和管理器工作。
  上游源码同时保留了 [KernelSU-Next](https://github.com/snothin/KernelSU-Next) 集成。
- [ghostlock-s26](https://github.com/snothin/ghostlock-s26)、@diabl0w 的 Z Fold8 移植输入，
  以及 [DFRoot](https://github.com/diabl0w/DFRoot)：上游利用链和公开 DirtyFrag 技术来源。

本项目用于你拥有并控制设备的安全研究。内核利用可能导致崩溃或重启，
系统更新后需重新审查兼容性。许可为 [Apache-2.0](LICENSE)，
第三方来源声明与致谢保留在 [NOTICE](NOTICE)。
