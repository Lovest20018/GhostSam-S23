#!/usr/bin/env python3
"""Publish a verified S23 draft; published releases are read-only on retries."""

import json
import os
from pathlib import Path
import re
import subprocess
import sys


STABLE_TAG = re.compile(r"s23-v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)")


def stable_version(tag: str) -> tuple[int, int, int] | None:
    match = STABLE_TAG.fullmatch(tag)
    return tuple(map(int, match.groups())) if match else None


def github_api(*args):
    result = subprocess.run(
        ["gh", "api", *args], check=True, capture_output=True, text=True,
    )
    return json.loads(result.stdout)


def validate_identity(release, tag, source, prerelease, publisher):
    for field, expected in {
        "tag_name": tag, "target_commitish": source, "prerelease": prerelease,
    }.items():
        if release[field] != expected:
            raise ValueError(f"Release {field} does not match the verified build")
    if not prerelease and release["author"]["login"] != publisher:
        raise ValueError("Full release must belong to the repository publisher")


def publish_verified(release, repository, tag, source, prerelease, publisher,
                     request=github_api):
    """Called only after the workflow has verified all three release assets."""
    validate_identity(release, tag, source, prerelease, publisher)
    if not release["draft"]:
        # In particular, never PATCH make_latest for a previously published tag.
        return release

    make_latest = False
    if not prerelease:
        version = stable_version(tag)
        if version is None:
            raise ValueError("Full release requires an independent stable S23 tag")
        pages = request(f"repos/{repository}/releases?per_page=100", "--paginate", "--slurp")
        published_versions = [
            parsed
            for page in pages for item in page
            if not item["draft"] and not item["prerelease"]
            if (parsed := stable_version(item["tag_name"])) is not None
        ]
        make_latest = all(version > previous for previous in published_versions)

    result = request(
        "--method", "PATCH", f"repos/{repository}/releases/{release['id']}",
        "-F", "draft=false", "-f", f"make_latest={str(make_latest).lower()}",
    )
    validate_identity(result, tag, source, prerelease, publisher)
    if result["draft"]:
        raise ValueError("Release remained a draft after publication")
    return result


def main() -> int:
    try:
        snapshot = Path("release.json")
        release = json.loads(snapshot.read_text(encoding="utf-8"))
        was_draft = release["draft"]
        result = publish_verified(
            release, os.environ["GITHUB_REPOSITORY"], os.environ["RELEASE_TAG"],
            os.environ["GITHUB_SHA"], os.environ["S23_PRERELEASE"] == "true",
            os.environ["S23_PUBLISHER"],
        )
        snapshot.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        action = "Published" if was_draft else "Verified existing release; left publication and Latest unchanged:"
        summary = f"{action} [{result['tag_name']}]({result['html_url']}) with three verified assets.\n"
        Path(os.environ["GITHUB_STEP_SUMMARY"]).write_text(summary, encoding="utf-8")
        print(summary, end="")
    except (KeyError, OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"s23-publish: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
