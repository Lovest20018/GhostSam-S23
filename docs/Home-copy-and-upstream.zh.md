# S23 首页文案与上游同步说明

更新日期：2026-11-10。

本文描述独立维护的 S23 产品主线 `main`。本次发布源码为 `0.3.0`（build 66），
独立产品版本与发布规则见 [S23-product-and-releases.zh.md](S23-product-and-releases.zh.md)。
最新已发布版本仍是 `0.2.3-s23-uapi5-r3`（build 63）。
[r3 测试版](https://github.com/Lovest20018/GhostSam-S23/releases/tag/s23-ksu-3.3.1-r3)
已由 [Actions #6](https://github.com/Lovest20018/GhostSam-S23/actions/runs/38679062368)
完整构建并发布，源码为 `14f1343`；r3 真机验证尚未进行。

仓库中英文首页均以 **GhostSam S23** 为项目展示名，介绍 S23 的目标固件、
授权流程和构建方式；应用显示名和首页标题也使用此名称。
GitHub 仓库路径为 `Lovest20018/GhostSam-S23`。

## 当前首页

S23 构建固定使用 standalone 路径和官方 KernelSU。首页标题为 **GhostSam S23**。
首页状态、主卡点击动作和授权提示由同一个状态映射确定。

| 条件 | 主卡标题 | 主卡说明 | 点击行为 |
| --- | --- | --- | --- |
| 已安装管理器，正在检测本应用 su | 正在检查授权 | 请稍候，正在确认 KernelSU 和本应用 su 权限 | 等待检测，不启动执行 |
| 未安装管理器 | 请先安装 KernelSU | 安装官方 KernelSU Manager 3.3.1 后返回 | 不启动执行 |
| 未确认加载，本应用没有 su | 准备获取临时 root | 首次使用或完整重启后，点击开始 | 启动执行；启用自定义文件时先选择文件 |
| 正在执行 | 正在获取临时 root | 请保持等待，进度见执行日志 | 进入执行页，不启动第二次执行 |
| 已确认加载，本应用没有 su | 已加载，等待授权 | 点击打开 KSU，在“超级用户”中授权本应用 | 打开 KSU |
| 已验证本应用 su 可用 | KernelSU 已启用 | 点击打开 KSU；可在管理器中进行软重启 | 打开 KSU |

执行中状态优先于其他状态，避免模块先加载、执行尚未结束时跳到管理器。
检测未完成前不提供执行动作，避免刚打开首页时误点。

未确认加载时，授权提示卡显示：

> 如果 KSU 已显示“已越狱”，无需再次执行。点击打开 KSU，在“超级用户”中
> 授予 GhostSam S23 su 权限，再返回确认状态。

已确认加载、但本应用未获得 su 时，授权提示卡显示：

> KernelSU 已加载。点击打开 KSU，在“超级用户”中授予 GhostSam S23 su 权限后返回。
> 显示“已启用”后，可从首页打开 KSU 进行软重启。

提示卡启动管理器的首页，用户再进入“超级用户”；当前未使用未经验证的
管理器内部页面跳转链接。

“已加载”的证据来自已有模块探测、本次开机记录或当前执行成功结果；
仅凭一次 su 检测失败不能判断模块不存在。无加载证据时使用条件提示，
不误报“已经加载”或“必须再次执行”。

授权后返回首页会重新检测 su，验证成功后同步启用状态和本次开机记录。
设备信息、SELinux、系统工具和可选的自定义文件配置入口继续保留。

## 重新打开后的状态恢复（build 65）

用户反馈：build 64 已成功获取 root，且已在 KernelSU 中授权 GhostSam S23，
但退出后重新打开仍显示“准备执行”。代码检查发现：

- 本地 su 原先只在启动或特定回调中检查一次；首次失败后，后续轮询依赖
  ADB/Shizuku 或旧记录，standalone 模式不能独立等待 su 就绪。
- standalone 已确认模块加载、但当时应用 su 尚未通过时，只在当前 ViewModel
  中保存成功状态；不写入模块加载记录，进程重建后会丢失这项证据。
- 回调式 ON_RESUME 监听在页面已进入前台后才注册时，会错过首次刷新。

build 65 的处理：

- 前台状态使用 `repeatOnLifecycle(RESUMED)`，每次进入前台强制实测
  `/system/bin/su -c id`；失败后每 5 秒重试，成功后停止本轮重试。
  不要求连接 ADB 或 Shizuku，离开前台取消轮询。
- 模块加载成功立即单独保存 `module_loaded_boot_id`，不以应用当时是否有 su
  为条件；同一次内核启动中可跨应用进程恢复，完整重启后因 boot_id 改变失效。
- 模块记录只用于“已加载，等待授权”，不会据此显示“KernelSU 已启用”。
  后者始终要求当前进程实际验证 su 成功。重新进入前台也会绕过 su 缓存。
- 本地 su 结果由一条监测任务更新，避免另一条启动探测把刚检测到的成功覆盖成失败。
- 保留 build 64 的 DirtyFrag、KernelSU 和固件检查策略，本次只修复应用状态恢复。

新增回归测试覆盖无外部通道时重试、加载记录恢复后等待授权、延后加载、
三星隐藏模块目录、前台重新检测授权、取消和恢复轮询；本地共 17 项 Kotlin
和 25 项 Python 测试通过。

最终源码 `7fe1fdcbeb306f7f67bf2b0fc46bf7003ffea847` 的
[完整 CI](https://github.com/Lovest20018/GhostSam-S23/actions/runs/42744246916)
已通过版本检查、驱动和 ksud 构建、APK 单元测试、打包及签名和载荷校验。
下载后再次核对 artifact 摘要、`SHA256SUMS`、包名、build 65、APK v2 签名，
并确认 ksud 与本次构建输入一致、DFR 模块与固定载荷一致。
APK SHA-256：`b3b3a468ed1c92e82bffabad6d6b82c5a45ba06b24570f012733b35a1efa94f4`。

该包使用本次 CI 生成的 debug 签名，与 build 64 不同。安装前需卸载旧测试应用，
安装后重新在 KSU 授权 GhostSam S23，再关闭并重开确认首页状态；
手机现有 root 可以保留，无需为此再次执行获取 root。

2026-11-10，用户安装本次 build 65 后反馈“没什么问题了”。结合本轮针对
重开状态的测试，记录为上述 SM-S9110 / FZJ1 设备的 root 与首页重开恢复正常。
该修复已纳入 `main`，中英文 README 指向此版本的已验证构建。
该反馈未覆盖 One UI 9 或完整重启后的全部流程。

## 文件隔离

| 用途 | 文件 |
| --- | --- |
| S23 状态与动作 | `app/app/src/main/java/com/snothin/ghostsam/ui/screen/s23/S23HomeState.kt` |
| S23 主卡与授权提示 | `app/app/src/main/java/com/snothin/ghostsam/ui/screen/s23/S23HomeCard.kt` |
| S23 英文文案 | `app/app/src/main/res/values/strings_s23.xml` |
| S23 中文文案 | `app/app/src/main/res/values-zh-rCN/strings_s23.xml` |
| 通用页面接入点 | `app/app/src/main/java/com/snothin/ghostsam/ui/screen/HomeScreen.kt` |
| 通用授权返回刷新 | `app/app/src/main/java/com/snothin/ghostsam/ui/screen/HomeEvidence.kt` |

使用独立资源名，不增加重复定义的 source set。S23 的目标不匹配说明和
管理器版本说明也移入上述资源文件。通用首页被改动的三条授权相关文案已恢复为
上游基线值，`HomeCards.kt` 保持上游实现。共享的模块加载日志仍在通用文案中，
因为它与通用 standalone 结果处理对应。

## 本地分支与基线

- `origin`：用户 fork `Lovest20018/GhostSam-S23`。
- `upstream`：原作者仓库 `snothin/GhostSam`。
- `main`：独立维护的 S23 产品主线，按自己的版本与验证结果发布。
- `upstream/main`：fetch 刷新的上游引用；`upstream-track` 是此前保留的本地基线分支。
- `s23/maintenance`：保留此前整理的本地分支，后续同步从最新 `main` 开始。
- 上游基线：`8fa2f68c39119eeea46f059057fecc5f371ee427`，v0.2.4。
- 既有源码保存提交：`5492115`。本轮整理在其后单独提交。
- 最近已审查并合并：`8fa2f68c39119eeea46f059057fecc5f371ee427`，v0.2.4，2026-11-10。
- [合并记录](upstream/2026-11-10-v0.2.4-merge.zh.md)说明组件配套关系、保留的 S23 定制及验证范围。

机器可读的基线、目标设备、KernelSU 固定版本及重点审查路径保存于
`maintenance/s23-baseline.json`。

当前 KernelSU 继续固定为 `snothin/KernelSU` 的
`b086147e79e2725177d960e7161ba279288efc9d`，UAPI 5、Manager 3.3.1、
KMI `android13-5.15`。build 64 合并上游时已同步该版本；build 65 的状态恢复修复
保留此版本及 build 64 的驱动、ksud 源码和载荷。

## 后续审查与选择性移植

在仓库根目录运行：

```powershell
python scripts/check-upstream.py --fetch
```

不访问网络、只检查本地已经获取的上游引用：

```powershell
python scripts/check-upstream.py --json
```

检查器验证上游地址和祖先关系，分别列出整合基线、已审查位置、新增待审查提交，
以及从整合基线起仍需评估的文件。已审查的未采用改动不会从基线差异中消失。
它只生成报告，不执行合并、不改源码、不移动维护分支。报告不是兼容性验证。

决定采用某项改动后，先保存工作目录，再在临时分支移植。下列分支名称仅作示例：

```powershell
git switch main
git status --short
git switch -c s23/upstream-review
```

在此分支选择需要的提交或补丁；遇到包含无关变化的大提交，按组件移植并记录来源。
工作目录有未保存改动时先处理，不能直接覆盖。移植后检查 S23 目标限制、
首页状态和点击动作、standalone 结果处理、资源、构建脚本及 payload 校验。
验证通过后，将临时分支的结果合并回 `main` 并推送；不要把 S23 提交写入 `upstream-track`。
在 `maintenance/s23-baseline.json` 更新 `reviewed_commit` 与审查记录。
选择性移植的改动写入 `adopted_changes`，记录上游来源、本地提交和验证结果。
只有完整接受对应上游历史时才移动 `baseline`；仅完成审查或采用少量补丁不能
把整版标为已合入。不要把 S23 提交写入上游跟踪分支。

应用与 Samsung KernelSU 属于两份独立上游。应用合并期间继续保留固定 KernelSU
提交；内核升级另行核对接口并同源重建产物。

## 构建与验证

`Build GhostSam S23` 对 `main` 推送、PR 与手动运行执行构建和测试，只产出
`ghostsam-s23-apk` artifact。新 `s23-v<versionName>` 标签通过版本、发布说明、
完整构建、载荷与签名检查后发布：无后缀版本为正式 Release，候选版本为 prerelease。
开发版本不能发布，历史 r2/r3 不重发。
S23 版本取自 `app/s23-version.properties`，构建不修改版本文件。

完整 Android 构建仍需要仓库指定的 JDK 21、SDK 37.0、NDK 30、CMake 及同源
UAPI 5 payload。不能使用旧资产跳过检查：

```powershell
cd app
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -Ps23Only=true --no-daemon --no-configuration-cache
```

验证覆盖首页状态和动作测试、S23 目标机型测试、独立资源与通用文案检查、
APK 打包校验测试、上游检查器的隔离 Git 仓库测试，以及 Android 资源编译。
完整 APK 构建和真机验证结果需另行记录，不能由这些检查推定成功。

真机仍需覆盖：首次执行、未授权重开、授权后返回、从 GhostSam 打开 KSU、
软重启、完整重启后重新获取权限、目标信息不匹配时停止执行。

原工作目录缺失的 `app/app/src/main/assets/payloads/s26/preload.so` 是本轮之前
已有的删除，保存基线及本轮提交均不包含该删除记录。

## 0.3.0 图标

S23 使用独立的 `ic_s23_launcher` 自适应图标，保留小幽灵造型，采用深墨绿底
与薄荷白主体。图形位于自适应安全区域内，眼睛采用透明镂空，单色主题中仍能辨认。
圆角方形、圆形与单色预览，以及 64/48/32 像素显示效果已检查。
Gradle 通过 manifest 占位符仅为 S23 选择新图标，通用构建继续使用原图标。
