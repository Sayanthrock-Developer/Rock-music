#!/usr/bin/env python3
"""Validate the repository's GitHub App manifest without using external packages."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

MANIFEST = Path(".github/github-app-manifest.json")
EXPECTED_PERMISSIONS = {
    "actions": "read",
    "checks": "read",
    "contents": "read",
    "issues": "read",
    "metadata": "read",
    "pull_requests": "read",
}
EXPECTED_EVENTS = {
    "check_run",
    "pull_request",
    "release",
    "workflow_run",
}
PLACEHOLDER_HOSTS = ("example.invalid", "YOUR_BACKEND")
FORBIDDEN_SECRET_MARKERS = (
    "-----BEGIN PRIVATE KEY-----",
    "-----BEGIN RSA PRIVATE KEY-----",
    "github_pat_",
    "ghp_",
    "ghs_",
)
FORBIDDEN_KEYS = {
    "private_key",
    "private_key_pem",
    "client_secret",
    "webhook_secret",
    "installation_token",
    "access_token",
}


def fail(message: str) -> None:
    print(f"GitHub App manifest validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def all_strings(value: Any) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        result: list[str] = []
        for item in value:
            result.extend(all_strings(item))
        return result
    if isinstance(value, dict):
        result = []
        for key, item in value.items():
            if str(key).lower() in FORBIDDEN_KEYS:
                fail(f"forbidden secret-bearing key '{key}' is present")
            result.extend(all_strings(item))
        return result
    return []


def is_placeholder(value: str) -> bool:
    return any(marker.lower() in value.lower() for marker in PLACEHOLDER_HOSTS)


def main() -> None:
    if not MANIFEST.is_file():
        fail(f"missing {MANIFEST}")

    try:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        fail(f"invalid JSON: {error}")

    if manifest.get("public") is not False:
        fail("the initial Rock Music GitHub App must remain private")

    if manifest.get("default_permissions") != EXPECTED_PERMISSIONS:
        fail("repository permissions must exactly match the approved read-only set")

    events = manifest.get("default_events")
    if not isinstance(events, list) or set(events) != EXPECTED_EVENTS or len(events) != len(EXPECTED_EVENTS):
        fail("webhook events must exactly match the approved event set")

    hook = manifest.get("hook_attributes")
    if not isinstance(hook, dict):
        fail("hook_attributes must be an object")

    endpoint_values = [
        manifest.get("redirect_url", ""),
        manifest.get("setup_url", ""),
        hook.get("url", ""),
        *manifest.get("callback_urls", []),
    ]
    placeholders_present = any(
        isinstance(value, str) and is_placeholder(value)
        for value in endpoint_values
    )

    if placeholders_present and hook.get("active") is not False:
        fail("placeholder endpoints require hook_attributes.active=false")

    if placeholders_present and manifest.get("setup_on_update") is not False:
        fail("placeholder endpoints require setup_on_update=false")

    for value in all_strings(manifest):
        if any(marker in value for marker in FORBIDDEN_SECRET_MARKERS):
            fail("a credential or private-key marker is present")

    print("GitHub App manifest validation passed.")


if __name__ == "__main__":
    main()
