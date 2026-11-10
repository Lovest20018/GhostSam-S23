# GhostSam S23：KernelSU Manager 3.3.1 兼容修复

当前开发源码面向以下设备范围，固件编号和完整内核版本不再精确匹配：

| 项目 | 目标 |
| --- | --- |
| 机型 | 国行 Galaxy S23，SM-S9110 |
| 固件与编译编号 | 不再作为白名单条件 |
| 系统 | Android 16 及后续版本（SDK ≥ 36） |
| 完整内核版本 | 不再精确匹配；保留内核窗口及运行前探测 |
| KMI | android13-5.15 |
| 管理器 | 官方 KernelSU Manager 3.3.1 |

Android 系统版本不决定 KMI。该手机升级到 Android 16 后仍使用
android13-5.15 内核，因此不能打包 android16-6.12 模块。

2026-11-10 用户提供的新设备信息为 One UI 8.5 / Android 16 / 编译编号
`BP4A.251205.006.S9110ZCS8FZJ1`，完整内核为
`5.15.189-android13-8-3251900-abS9110ZCS8FZJ1`。
旧 r3 只接受 FZI1，因此在执行前拒绝这台 FZJ1 设备；该日志不代表漏洞或
KernelSU 加载已失败。开发源码现已去掉编译编号、完整内核版本和 SDK 上限，
允许这类更新进入兼容性检查。新日志记录设备实际编译编号、SDK 和安全补丁日期。

本包仍只包含 `android13-5.15` 的 helper 与内嵌 KernelSU 模块；KMI 不同会
明确报告缺少对应模块。保留内核窗口、页缓存探测和资产检查。
One UI 9 等后续系统不会仅因系统编号被拒绝，但其内核、漏洞补丁和模块兼容性
仍需实测。用户已反馈 build 64 在 FZJ1 获取 root 正常，但重开后首页状态复位；
build 65 修复后尚待真机回归，One UI 9 未验证。
该策略参考 Android 的 [KMI 稳定性说明](https://source.android.com/docs/core/architecture/kernel/stable-kmi)，
移除字符串白名单不等于不同 KMI 的模块可以互换。

## 修复内容

Manager 3.3.1 要求 UAPI 5，而原 APK 的定制 ksud 及其内嵌 KernelSU
模块使用 UAPI 2，导致首页显示“需要更新内核”。升级管理器不会替换它们。

2026-11-10 合并 GhostSam v0.2.4 后，构建固定 `snothin/KernelSU` 的
`kdp-s26` 分支提交 `b086147e79e2725177d960e7161ba279288efc9d`。
该提交原生提供 UAPI 5、su 会话 FD、EVENT_SERVICES、app profile v4
以及 Manager task_work 安装监听，因此不再应用 r3 基线的 UAPI/监听回移补丁。
旧补丁文件保留用于追溯历史构建。

`scripts/prepare-kernelsu.py` 校验指定提交、工作区与关键源码摘要，拒绝
其他提交和已修改的受跟踪源码。随后从同一份源码重建 ksud 及唯一的
`android13-5.15_kernelsu.ko`，保留三星 KDP/DEFEX、预置 daemon 的 staging
和加载后的 `finish_install()`。DirtyFrag 无预置文件时继续使用其自安装路径。
新版 ksud 在 late-load 后统一设置分区只读，配套采用上游移除
`--ro-partitions` 参数后的 helper。

新版用 `CONFIG_KSU_RKP_NO_PATCH_TEXT` 取代旧三星 RKP 开关。构建启用
KDP、DEFEX 和该 RKP 兼容路径，并验证 `setresuid_kretprobe` 与
`rkp_sucompat_execve_kprobe` 已编译，保留 dispatcher 不可用时的备用 hook。
是否在目标固件成功注册仍需要真机验证，不能仅凭模块存在判断。

首次日志只记录 native `kernelsu=up`，未记录应用的 `su -c id` 成功，
却打印“uid=0”。第二版区分模块加载与实测 su 授权，修正该误报和日志
中遗留的 3.3.0 标签。管理器身份是否就绪仍以 Manager 或内核日志为准。

用户日志的 `soft_reboot=1` 对应“设置 → 更多设置 → 自动软重启”开关。
这是 ksud 主动重启 Android 服务；排查管理器连接时可关闭，再完整重启
手机并运行 GhostSam。第二版保留用户的开关选择。

专用 APK 使用 DirtyFrag standalone 路径，固定官方 KernelSU，不提供
Ghostlock 或 KernelSU-Next 切换。无需把 S23 填进原系列表或猜测偏移。
打包只保留 ksud 和 `dfr_lkm-android13-5.15.ko`；后者是安装辅助模块，
不是内嵌的 KernelSU 驱动；本次合并采用 v0.2.4 的配套 helper。

APK 显示名称为 **GhostSam S23**，包名为 `com.snothin.ghostsam.s23`，
可与原 GhostSam 同时安装。开始执行前核对机型、最低 SDK、KMI 和运行条件。
不再因编译编号或完整内核字符串变化而停止；系统更新后的可用性仍需实测。

## 在 fork 中构建

1. 当前 S23 源码已合入 fork 的 `main`。从 Actions 选择
   **Build GhostSam S23**，在 **Run workflow** 中选择 `main` 手动构建。
   `main` 的 push 和 PR 也会触发构建。
2. 工作流依次构建 Samsung KernelSU 模块、嵌入该模块的 ksud、S23 APK。
   APK 构建使用 JDK 21、SDK 37.0、NDK 30.0.16248370 和 CMake 3.22.1。
3. 成功后下载 `ghostsam-s23-apk` artifact，内含
   `GhostSam-S23.apk`、构建信息和 `SHA256SUMS`。
   在解压目录执行 `sha256sum -c SHA256SUMS` 核对文件。
   `main` 推送、PR 和手动构建只生成 `ghostsam-s23-apk` artifact。
   新版按 `s23-v<versionName>` 标签独立发布测试版；详见
   [产品与发布规则](S23-product-and-releases.zh.md)。历史 r2/r3 不重发。

当前源码为 `0.3.0-dev`（build 65），独立维护产品与发布流程，
并已移除固件与完整内核精确匹配、合并 v0.2.4 的 DirtyFrag 和新版 KernelSU。
组件来源与验证范围见[合并记录](upstream/2026-11-10-v0.2.4-merge.zh.md)。
build 65 修复退出应用后首页状态恢复，说明见[首页状态维护记录](Home-copy-and-upstream.zh.md#重新打开后的状态恢复build-65)。
最新已发布的 [r3 测试版](https://github.com/Lovest20018/GhostSam/releases/tag/s23-ksu-3.3.1-r3)
已从 `14f1343` 完整构建并发布，CI 构建、测试、包名与版本、载荷和签名检查通过。
r3 APK 尚未进行真机验证；当前开发源码尚未发布。

APK 是标准 debug 签名的测试版本。工作流验证包名、APK 签名、两个 payload
与本次构建输入完全一致，以及 arm64 standalone JNI 库存在。
不需要上传私有 release 签名密钥。不同环境的 debug 签名可能不同，后续
升级安装需要保持同一签名，或先卸载测试版再安装。

本地构建需要先用上述流程生成 UAPI 5 ksud，再覆盖
`app/app/src/main/assets/payloads/ksud/ksud`，执行：

```bash
cd app
./gradlew :app:testDebugUnitTest :app:assembleDebug -Ps23Only=true --no-daemon --no-configuration-cache
```

`verifyPayloads` 拒绝旧 UAPI 的 ksud 和错误格式的辅助模块。
`scripts/verify-s23-apk.py` 检查最终 APK，拒绝旧资产、其他 KMI、Next 和
companion APK。版本标记用于发现误打包；兼容性仍依靠源码核对、同源编译
和真机验证。

## 真机验证

1. 安装 S23 专用 APK 和官方 KernelSU Manager 3.3.1。
2. **重启手机**后运行 GhostSam S23 的 standalone 安装流程。
   late-load 不会替换本次启动已经加载的旧 KernelSU，重启才能切换驱动。
3. 检查 KernelSU Manager 是否识别驱动且不再显示“需要更新内核”。
   验证 root 授权、`su -c id`、授权名单及现有模块。
4. 如果失败，保存完整 GhostSam 日志及管理器截图，特别是模块加载、
   KDP/DEFEX 和内核接口有关的错误。

源码测试和 Actions 成功不等于该固件已通过真机测试。5.15 在现有
DirtyFrag 兼容窗口中属于预测支持；三星厂商内核配置和安全补丁是否允许
安装、模块初始化是否成功，需要这台 SM-S9110 实测确认。
