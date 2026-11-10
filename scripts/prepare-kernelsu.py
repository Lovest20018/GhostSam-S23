#!/usr/bin/env python3
"""Validate the pinned, native-UAPI-5 Samsung KernelSU source before rebuilding."""

import argparse
import hashlib
from pathlib import Path
import subprocess
import sys


KERNELSU_REPOSITORY = "https://github.com/snothin/KernelSU.git"
KERNELSU_REF = "b086147e79e2725177d960e7161ba279288efc9d"
KERNELSU_BRANCH = "kdp-s26"
UAPI_VERSION = 5
# The new pin already includes UAPI 5 and the task_work observer. Old backports
# must not be applied to it. Hashes use normalized LF text on every host.
SOURCE_HASHES = {
    "uapi/supercall.h": "9ec7d55185d9d76ff3d1dca8b37f4658bc3d6021d987891d153300420df90a2c",
    "kernel/manager/pkg_observer.c": "1d5d6e37c111f724eab1166c73184ad1163851e4b769160689163e98c9e21598",
    "kernel/hook/syscall_hook_manager.c": "9e59af80705d2c5b38cb68d1a5c6b06394aba8610d97a392555a27841e095076",
    "kernel/Kbuild": "6da4c5038d23cb3962f9f517a7d05c6474df2751d1f1c4aeb8148311f503044a",
    "userspace/ksud/src/cli.rs": "4a389f45123aaecdd3285e412a860cb3ec6075b860f85a40f77ae516d30c0914",
    "userspace/ksud/src/late_load.rs": "0b8e85fcd6c05f84df0332a2f6739338ea82c4e995b4c2158fa0ae52a6cb2f40",
}


def git(source: Path, *args: str) -> bytes:
    result = subprocess.run(
        ["git", "-C", str(source), *args], capture_output=True, check=False
    )
    if result.returncode:
        detail = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"git {' '.join(args)} failed: {detail}")
    return result.stdout


def read_source(source: Path, relative: str) -> str:
    return (source / relative).read_text(encoding="utf-8")


def validate_other_changes(source: Path) -> None:
    # Inspect both index and worktree; comparing only HEAD can hide staged changes.
    changes = set()
    for args in (("diff", "--name-only", "-z"), ("diff", "--cached", "--name-only", "-z")):
        changes.update(git(source, *args).split(b"\0"))
    changes.discard(b"")
    if changes:
        paths = ", ".join(sorted(p.decode("utf-8", errors="replace") for p in changes))
        raise RuntimeError(f"Refusing modified pinned KernelSU source: {paths}")


def prepare(source: Path) -> None:
    source = source.resolve(strict=True)
    root = Path(git(source, "rev-parse", "--show-toplevel").decode().strip()).resolve()
    if source != root:
        raise RuntimeError("Pass the KernelSU repository root, not a subdirectory")
    actual_ref = git(source, "rev-parse", "HEAD").decode().strip()
    if actual_ref != KERNELSU_REF:
        raise RuntimeError(f"Expected KernelSU HEAD {KERNELSU_REF}, found {actual_ref}")

    validate_other_changes(source)
    for relative, expected_digest in SOURCE_HASHES.items():
        digest = hashlib.sha256(read_source(source, relative).encode("utf-8")).hexdigest()
        if digest != expected_digest:
            raise RuntimeError(f"Pinned source contract does not match: {relative}")

    required = {
        "uapi/supercall.h": ("KERNEL_SU_UAPI_VERSION = 5;", "EVENT_SERVICES = 4;"),
        "kernel/supercall/supercall.c": ("[ksu_driver_su]", "ksu_install_su_fd"),
        "kernel/supercall/dispatch.c": ("case EVENT_SERVICES:", "services_started"),
        "userspace/ksud/src/ksucalls.rs": ("pub fn report_services()", "SU_DRIVER_FD_NAME"),
        "uapi/app_profile.h": ("#define KSU_APP_PROFILE_VER 4",),
        "kernel/hook/syscall_hook_manager.c": (
            "CONFIG_KSU_RKP_NO_PATCH_TEXT", "rkp_setresuid_hook_init",
            "rkp_sucompat_execve_kprobe",
        ),
        "kernel/Kbuild": ("-DCONFIG_KSU_RKP_NO_PATCH_TEXT=1",),
        "kernel/manager/pkg_observer.c": ("task_work_add(", "TWA_RESUME"),
        "userspace/ksud/src/cli.rs": ("ro_partitions: _ro_partitions", "utils::set_partitions_ro()"),
        "userspace/ksud/src/late_load.rs": ("utils::stage_daemon_from(stage_from)", "utils::finish_install(None)"),
    }
    for relative, markers in required.items():
        text = read_source(source, relative)
        if any(marker not in text for marker in markers):
            raise RuntimeError(f"Missing required UAPI 5 source support in {relative}")

    print(f"KernelSU source: {KERNELSU_REPOSITORY} ({KERNELSU_BRANCH}) @ {actual_ref}")
    print(f"KernelSU UAPI: {UAPI_VERSION}; native contract verified; no source patches applied")
    print("Manager installation observer: upstream 3.3.1 native task_work")
    print("Samsung build flags: KDP, DEFEX, RKP_NO_PATCH_TEXT")
    print("Rebuild ksud and every embedded kernelsu.ko from this validated source.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="KernelSU repository root")
    args = parser.parse_args()
    try:
        prepare(args.source)
    except (OSError, RuntimeError, UnicodeError) as error:
        print(f"prepare-kernelsu: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
