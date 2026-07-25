#!/usr/bin/env python3
"""Validate the repository's GitHub App manifest without external packages."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

DEFAULT_MANIFEST = Path(".github/github-app-manifest.json")
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
    "gho_",
    "ghu_",
    "ghs_",
    "ghr_",
)
FORBIDDEN_KEYS = {
    "private_key",
    "private_key_pem",
    "client_secret",
    "webhook_secret",
    "installation_token",
    "access_token",
}


class ValidationError(ValueError):
    """Raised when a GitHub App manifest violates the approved security model."""


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
                raise ValidationError(f"forbidden secret-bearing key '{key}' is present")
            result.extend(all_strings(item))
        return result
    return []


def is_placeholder(value: str) -> bool:
    return any(marker.lower() in value.lower() for marker in PLACEHOLDER_HOSTS)


def validate_manifest(manifest: dict[str, Any]) -> None:
    if manifest.get("public") is not False:
        raise ValidationError("the initial Rock Music GitHub App must remain private")

    if manifest.get("default_permissions") != EXPECTED_PERMISSIONS:
        raise ValidationError(
            "repository permissions must exactly match the approved read-only set",
        )

    events = manifest.get("default_events")
    if (
        not isinstance(events, list)
        or set(events) != EXPECTED_EVENTS
        or len(events) != len(EXPECTED_EVENTS)
    ):
        raise ValidationError("webhook events must exactly match the approved event set")

    hook = manifest.get("hook_attributes")
    if not isinstance(hook, dict):
        raise ValidationError("hook_attributes must be an object")

    callback_urls = manifest.get("callback_urls", [])
    if not isinstance(callback_urls, list):
        raise ValidationError("callback_urls must be an array")

    endpoint_values = [
        manifest.get("redirect_url", ""),
        manifest.get("setup_url", ""),
        hook.get("url", ""),
        *callback_urls,
    ]
    placeholders_present = any(
        isinstance(value, str) and is_placeholder(value)
        for value in endpoint_values
    )

    if placeholders_present and hook.get("active") is not False:
        raise ValidationError("placeholder endpoints require hook_attributes.active=false")

    if placeholders_present and manifest.get("setup_on_update") is not False:
        raise ValidationError("placeholder endpoints require setup_on_update=false")

    for value in all_strings(manifest):
        if any(marker in value for marker in FORBIDDEN_SECRET_MARKERS):
            raise ValidationError("a credential or private-key marker is present")


def load_manifest(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise ValidationError(f"missing {path}")

    try:
        parsed = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        raise ValidationError(f"invalid JSON: {error}") from error

    if not isinstance(parsed, dict):
        raise ValidationError("manifest root must be a JSON object")
    return parsed


def main() -> None:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_MANIFEST
    try:
        validate_manifest(load_manifest(path))
    except ValidationError as error:
        print(f"GitHub App manifest validation failed: {error}", file=sys.stderr)
        raise SystemExit(1) from error

    print("GitHub App manifest validation passed.")


if __name__ == "__main__":
    main()
