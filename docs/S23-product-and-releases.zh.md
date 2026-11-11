# GhostSam S23：独立产品与发布规则

生效日期：2026-10-16；2026-11-11 更新正式 Release 与重跑策略。

GhostSam S23 按目标 S23 固件的使用需求、验证结果和维护进度独立发布。
保留 GhostSam 的来源、许可证、Git 历史和上游引用，按需采用上游修复。
上游发布新版本本身不构成 S23 发版理由。

## 产品与版本

- 产品名：GhostSam S23；包名：`com.snothin.ghostsam.s23`。
- 主线：`main`；默认 Gradle 构建为 S23，原有通用构建需显式 `-Ps23Only=false`。
- 版本唯一来源：`app/s23-version.properties`，不继承 `app/version.properties`。
- 当前待发布版：`0.3.1`，`versionCode=67`；沿用 0.3.0 的应用逻辑，修复发布流程的 Latest 回退。
- 当前开发源码不再精确匹配固件或完整内核版本，Android 要求为 SDK ≥ 36；
  仍检查 SM-S9110、`android13-5.15` KMI 和运行前兼容条件。
  FZJ1 已收到 build 64 获取 root 成功的反馈，但存在重开应用后首页状态复位的问题，
  build 65 修复后，用户已于 2026-11-10 反馈 root 及重开状态正常；One UI 9 未验证。
  已发布 r3 仍保留旧版限制。
- 最新已发布：[0.3.0（build 66）](https://github.com/Lovest20018/GhostSam-S23/releases/tag/s23-v0.3.0)，正式 Release；历史 r2/r3 附件继续保留。
- `versionCode` 由维护者随可分发更新显式递增，必须高于最新已发布版本。
  构建、测试和重跑 CI 均不修改版本文件。
- 标签为 `s23-v<versionName>`，例如候选版本 `0.3.0-rc.1` 对应
  `s23-v0.3.0-rc.1`。版本号按本产品维护，不对应 GhostSam 或 KernelSU 版本。

无后缀版本（例如 `0.3.1`）发布为正式 Release；仅高于所有已发布 S23 正式版本时
标记为 GitHub Latest。已发布版本重跑只核对源码、发布者和附件，不修改 Latest。
`-alpha.N`、`-beta.N` 和 `-rc.N` 发布为 prerelease；`-dev` 只生成 artifact，
拒绝发布标签。正式 Release 的设备验证范围仍以发布说明记录为准。

## 构建与发布

`Build GhostSam S23` 保留原工作流路径 `kernelsu-331.yml`，避免已有链接失效。

| 触发方式 | 结果 |
| --- | --- |
| 推送 `main` / PR / 手动运行 | 从固定 KernelSU 源码完整构建、测试，生成 artifact |
| 推送匹配的 `s23-v…` 标签 | 先验证版本与发布说明，全部构建通过后按版本类型发布 |
| `-dev` 版本的发布标签 | 拒绝发布；开发版本只用于 artifact |
| 历史 `s23-ksu-3.3.1-r2/r3` 标签 | 不触发新发布流程；既有 Release 与附件保留 |

artifact 名称为 `ghostsam-s23-apk`，包含 `GhostSam-S23.apk`、`BUILD-INFO.txt`
与 `SHA256SUMS`。构建信息同时记录 S23 产品版本、构建源码、KernelSU 固定提交、
KMI、三星兼容配置及验证范围。历史 r3 artifact 与附件仍使用原名称。

准备发布时：

1. 记录目标设备的真机反馈和未覆盖的验证范围。
2. 在 `app/s23-version.properties` 设置正式版本（例如 `0.3.1`）或候选版本；
   build 高于已发布 APK 及此前分发的开发构建。
3. 新建对应 `docs/releases/s23-v<versionName>.zh.md`，记录变化、设备范围、
   构建与真机验证结果、安装方式和已知限制。
4. 推送与版本文件完全匹配的源码标签。工作流完整构建，
   核对 APK 版本、包名、载荷、签名、附件摘要及标签源码，再公开发布。
   正式 Release 由维护者 `Lovest20018` 先创建同标签的草稿，目标为该源码提交；
   工作流只向已校验的草稿补充缺失附件并公开发布，使 GitHub 发布者保持为维护者账号。
   维护者账号凭据不写入仓库或 CI。
5. 发布成功后，在进入下一开发版本的提交中更新维护清单的最新已发布版本、
   README 下载说明，并把版本文件切换到更高 build 的下一 `-dev` 版本。

本地预检：

```bash
python scripts/s23-release.py
python scripts/s23-release.py --tag s23-v0.3.1
python -m unittest discover -s tests -p 'test_*.py' -v
```

第二条仅在版本文件与标签一致且对应发布说明存在时通过。
发布脚本保留既有 Release 附件的校验，不覆盖已发布文件。
发布任务串行执行；新草稿通过分页获取全部 Release 后比较数字版本，较旧草稿
不会替换较新版本的 Latest。查询失败时停止发布，不把网络错误当成没有历史版本。
此策略从 0.3.1 工作流生效，旧标签与历史 CI 定义不会被追溯修改，勿重跑旧发布任务。

包名继续兼容现有 S23 安装，但当前 CI 使用每次运行生成的 debug 签名；
版本号递增不消除签名差异。遇到签名冲突时仍按既有安装说明处理。

## 底层与上游

2026-11-10 合并 GhostSam v0.2.4（`8fa2f68`），采用配套的 DirtyFrag core、
loader 和 helper，删除上游已移除的 companion 流程。S23 APK 仍只打包
`android13-5.15` helper 与重建的官方 ksud。

KernelSU 固定 `b086147e79e2725177d960e7161ba279288efc9d`，原生提供 UAPI 5
和 Manager task_work 安装监听，不再应用旧基线的两个回移补丁。构建启用
KDP、DEFEX 和 `CONFIG_KSU_RKP_NO_PATCH_TEXT`，从同一份源码重建驱动与 ksud。
详见[合并记录](upstream/2026-11-10-v0.2.4-merge.zh.md)。

`maintenance/s23-baseline.json` 分别记录：

- `baseline`：实际整合的 GhostSam 基线，当前为 v0.2.4 / `8fa2f68`。
- `reviewed_commit`：最近已完成审查的上游位置，当前为 v0.2.4 / `8fa2f68`。
- `review_log`：审查结论与候选改动。
- `adopted_changes`：实际采用的来源与验证记录，包括本次 v0.2.4 合并。

合并不改变 S23 产品版本和包名。构建成功本身不构成目标固件已兼容的证据；
用户真机反馈及覆盖范围另行记录。
