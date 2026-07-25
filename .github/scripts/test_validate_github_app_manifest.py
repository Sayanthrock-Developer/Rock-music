from __future__ import annotations

import copy
import importlib.util
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("validate-github-app-manifest.py")
SPEC = importlib.util.spec_from_file_location("github_app_validator", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load validator from {MODULE_PATH}")
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


def safe_manifest() -> dict:
    return {
        "name": "Rock Music Sayanth",
        "url": "https://github.com/Sayanthrock-Developer/Rock-music",
        "description": "Read-only Rock Music GitHub App.",
        "public": False,
        "redirect_url": "https://rock-music.example.invalid/github/callback",
        "callback_urls": [
            "https://rock-music.example.invalid/github/callback",
        ],
        "setup_url": "https://rock-music.example.invalid/github/setup",
        "setup_on_update": False,
        "hook_attributes": {
            "url": "https://rock-music.example.invalid/github/webhook",
            "active": False,
        },
        "default_permissions": dict(VALIDATOR.EXPECTED_PERMISSIONS),
        "default_events": sorted(VALIDATOR.EXPECTED_EVENTS),
    }


class ManifestValidationTest(unittest.TestCase):
    def test_safe_inactive_placeholder_manifest_passes(self) -> None:
        VALIDATOR.validate_manifest(safe_manifest())

    def test_active_placeholder_webhook_fails(self) -> None:
        manifest = safe_manifest()
        manifest["hook_attributes"]["active"] = True
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "active=false"):
            VALIDATOR.validate_manifest(manifest)

    def test_placeholder_setup_redirect_fails(self) -> None:
        manifest = safe_manifest()
        manifest["setup_on_update"] = True
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "setup_on_update=false"):
            VALIDATOR.validate_manifest(manifest)

    def test_extra_permission_fails(self) -> None:
        manifest = safe_manifest()
        manifest["default_permissions"]["administration"] = "write"
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "permissions"):
            VALIDATOR.validate_manifest(manifest)

    def test_extra_event_fails(self) -> None:
        manifest = safe_manifest()
        manifest["default_events"].append("push")
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "events"):
            VALIDATOR.validate_manifest(manifest)

    def test_private_key_marker_fails(self) -> None:
        manifest = safe_manifest()
        manifest["description"] = "-----BEGIN PRIVATE KEY-----"
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "credential"):
            VALIDATOR.validate_manifest(manifest)

    def test_all_github_token_prefixes_fail(self) -> None:
        token_prefixes = (
            "github_pat_",
            "ghp_",
            "gho_",
            "ghu_",
            "ghs_",
            "ghr_",
        )
        for prefix in token_prefixes:
            with self.subTest(prefix=prefix):
                manifest = safe_manifest()
                manifest["description"] = f"{prefix}example"
                with self.assertRaisesRegex(VALIDATOR.ValidationError, "credential"):
                    VALIDATOR.validate_manifest(manifest)

    def test_secret_bearing_field_fails(self) -> None:
        manifest = safe_manifest()
        manifest["webhook_secret"] = "not-a-real-secret"
        with self.assertRaisesRegex(VALIDATOR.ValidationError, "secret-bearing"):
            VALIDATOR.validate_manifest(manifest)

    def test_real_https_endpoints_can_be_active(self) -> None:
        manifest = copy.deepcopy(safe_manifest())
        manifest["redirect_url"] = "https://backend.example.com/github/callback"
        manifest["callback_urls"] = ["https://backend.example.com/github/callback"]
        manifest["setup_url"] = "https://backend.example.com/github/setup"
        manifest["setup_on_update"] = True
        manifest["hook_attributes"] = {
            "url": "https://backend.example.com/github/webhook",
            "active": True,
        }
        VALIDATOR.validate_manifest(manifest)


if __name__ == "__main__":
    unittest.main()
