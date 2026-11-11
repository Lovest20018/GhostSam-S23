"""Verify publication retries and out-of-order drafts cannot downgrade Latest."""

import copy
import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import Mock


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/s23-publish.py"
SPEC = importlib.util.spec_from_file_location("s23_publish", SCRIPT)
publish = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(publish)


class S23PublishTest(unittest.TestCase):
    def setUp(self):
        self.release = {
            "id": 123, "tag_name": "s23-v0.3.1", "target_commitish": "a" * 40,
            "draft": True, "prerelease": False, "author": {"login": "Lovest20018"},
        }
        self.request = Mock()

    def run_publication(self):
        return publish.publish_verified(
            self.release, "Lovest20018/GhostSam-S23", "s23-v0.3.1", "a" * 40,
            False, "Lovest20018", request=self.request,
        )

    def prepare_responses(self, pages):
        self.request.side_effect = [pages, {**self.release, "draft": False}]

    @staticmethod
    def published(tag, **overrides):
        return {"tag_name": tag, "draft": False, "prerelease": False, **overrides}

    def assert_latest(self, expected):
        args = self.request.call_args.args
        self.assertEqual(args[:3], ("--method", "PATCH", "repos/Lovest20018/GhostSam-S23/releases/123"))
        self.assertIn("draft=false", args)
        self.assertIn(f"make_latest={str(expected).lower()}", args)

    def test_published_retry_makes_no_api_calls_or_changes(self):
        self.release["draft"] = False
        original = copy.deepcopy(self.release)
        result = self.run_publication()
        self.assertEqual(result, original)
        self.assertEqual(self.release, original)
        self.request.assert_not_called()

    def test_new_stable_release_becomes_latest(self):
        self.prepare_responses([[self.published("s23-v0.3.0")]])
        self.assertFalse(self.run_publication()["draft"])
        self.assert_latest(True)

    def test_first_stable_release_becomes_latest(self):
        self.prepare_responses([[]])
        self.run_publication()
        self.assert_latest(True)

    def test_older_draft_does_not_replace_newer_release_on_a_later_page(self):
        self.prepare_responses([
            [self.published("s23-v0.3.0")], [self.published("s23-v0.4.0")],
        ])
        self.run_publication()
        self.assert_latest(False)
        self.assertEqual(self.request.call_args_list[0].args[-2:], ("--paginate", "--slurp"))

    def test_version_order_is_numeric(self):
        self.release["tag_name"] = "s23-v0.10.0"
        self.prepare_responses([[self.published("s23-v0.9.0")]])
        publish.publish_verified(
            self.release, "Lovest20018/GhostSam-S23", "s23-v0.10.0", "a" * 40,
            False, "Lovest20018", request=self.request,
        )
        self.assert_latest(True)

    def test_unpublished_and_prerelease_versions_do_not_block_latest(self):
        self.prepare_responses([[
            self.published("s23-v1.0.0", draft=True),
            self.published("s23-v0.4.0-rc.1", prerelease=True),
            self.published("s23-ksu-3.3.1-r3", prerelease=True),
        ]])
        self.run_publication()
        self.assert_latest(True)

    def test_candidate_publication_never_changes_latest(self):
        self.release.update(tag_name="s23-v0.4.0-rc.1", prerelease=True)
        self.request.return_value = {**self.release, "draft": False}
        publish.publish_verified(
            self.release, "Lovest20018/GhostSam-S23", "s23-v0.4.0-rc.1", "a" * 40,
            True, "Lovest20018", request=self.request,
        )
        self.request.assert_called_once()
        self.assert_latest(False)

    def test_failed_release_listing_stops_before_any_patch(self):
        self.request.side_effect = subprocess.CalledProcessError(1, ["gh", "api"])
        with self.assertRaises(subprocess.CalledProcessError):
            self.run_publication()
        self.request.assert_called_once()
        self.assertNotIn("PATCH", self.request.call_args.args)

    def test_invalid_release_identity_is_rejected_without_api_calls(self):
        for patch in (
            {"tag_name": "s23-v0.3.0"}, {"target_commitish": "b" * 40},
            {"prerelease": True}, {"author": {"login": "unexpected"}},
        ):
            with self.subTest(patch=patch):
                original = self.release
                self.release = {**original, **patch}
                with self.assertRaises(ValueError):
                    self.run_publication()
                self.release = original
        self.request.assert_not_called()

    def test_publication_response_must_still_match_verified_identity(self):
        self.request.side_effect = [[[]], {**self.release, "draft": False, "target_commitish": "b" * 40}]
        with self.assertRaisesRegex(ValueError, "target_commitish"):
            self.run_publication()


if __name__ == "__main__":
    unittest.main()
