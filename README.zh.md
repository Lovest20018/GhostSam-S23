# GhostSam S23

**中文** | [English](README.md)

<img src="docs/assets/ghostsam-s23-icon.svg" width="64" height="64" alt="GhostSam S23 图标">

面向国行 **三星 Galaxy S23（SM-S9110）** 的临时 root 应用，
使用 DirtyFrag standalone 与官方 **KernelSU Manager 3.3.1 / UAPI 5**。

[下载 0.3.1 APK](https://github.com/Lovest20018/GhostSam-S23/releases/download/s23-v0.3.1/GhostSam-S23.apk)
· [发布说明](https://github.com/Lovest20018/GhostSam-S23/releases/tag/s23-v0.3.1)

## 支持范围

- **设备：** SM-S9110；Android 16 及后续版本；KMI `android13-5.15`。
- 不锁定固件编号和完整内核字符串，保留运行前兼容性检查。
- 用户已反馈 One UI 8.5 / Android 16 / `S9110ZCS8FZJ1` 上 root 和应用重开正常（build 65）。
- One UI 9 与其他 KMI 尚未验证。

## 使用方法

1. 安装 GhostSam S23 和官方 KernelSU Manager 3.3.1。
2. 首次使用或完整重启后，在 GhostSam S23 中获取临时 root。
3. 如果 KSU 已显示 **“已越狱”**，在 **“超级用户”** 中授予 **GhostSam S23** su 权限，再返回应用；验证后显示 **“KernelSU 已启用”**。

点击首页主卡可打开 KSU 使用软重启。完整重启后临时 root 失效，需重新获取。
应用重新打开时会再次检查授权，检测失败后自动重试。

不同 CI 构建的签名可能不同。若安装提示签名冲突，先保存需要的设置和日志，
卸载旧 GhostSam S23，安装新版并在 KSU 重新授权；手机现有 root 可以保留。

## 0.3.1 更新

- 修复发布重跑和旧草稿覆盖较新 GitHub Latest 的问题。
- 新增发布回归测试，发布任务串行执行。
- 保留 0.3.0 的 root 状态恢复、固件兼容策略和新版幽灵图标。

## 开发与文档

[构建说明](docs/KernelSU-3.3.1.zh.md) · [验证记录与上游维护](docs/Home-copy-and-upstream.zh.md) · [发布规则](docs/S23-product-and-releases.zh.md)

通过 [Actions → Build GhostSam S23](https://github.com/Lovest20018/GhostSam-S23/actions/workflows/kernelsu-331.yml) 构建。
版本独立维护于 `app/s23-version.properties`；Release 附带 `BUILD-INFO.txt` 和 `SHA256SUMS`。

由 [Lovest20018](https://github.com/Lovest20018) 维护，基于 [snothin/GhostSam](https://github.com/snothin/GhostSam) 与
[Samsung KernelSU](https://github.com/snothin/KernelSU)。
用于你拥有并控制的设备；[Apache-2.0](LICENSE)，完整致谢见 [NOTICE](NOTICE)。
