#!/usr/bin/env python3
"""Create or update a single PR comment with the GitHub Actions quality reports.
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

MARKER = "<!-- pr-quality -->"
BOT_LOGIN = "github-actions[bot]"
REPORTS_DIR = Path("quality-summaries")
MAX_COMMENT_BYTES = 55_000

STATUSES = {
    "success": "Passed",
    "failure": "Failed",
    "skipped": "Skipped",
    "cancelled": "Cancelled",
}


def required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Missing required environment variable: {name}")
    return value


def summary(filename: str) -> str:
    file = REPORTS_DIR / filename
    if not file.is_file():
        return "_Report unavailable. Check the workflow logs and artifacts._"
    return file.read_text(encoding="utf-8").strip() or "_Report is empty._"


def make_comment(run_url: str, verify: str, mutation: str) -> str:
    sections = [
        MARKER,
        "# PR Quality",
        "",
        "| Check | Result |",
        "| --- | --- |",
        f"| Verify | {STATUSES.get(verify, verify)} |",
        f"| Mutation testing | {STATUSES.get(mutation, mutation)} |",
        "",
        f"[Workflow run and artifacts]({run_url})",
        "",
        summary("verify-summary.md"),
        "",
        (
            "_Mutation testing was skipped because verification did not succeed._"
            if mutation == "skipped"
            else summary("mutation-summary.md")
        ),
    ]
    body = "\n".join(sections)
    encoded = body.encode("utf-8")
    if len(encoded) > MAX_COMMENT_BYTES:
        note = "\n\n_Comment truncated; full reports are available as workflow artifacts._"
        budget = MAX_COMMENT_BYTES - len(note.encode("utf-8"))
        body = encoded[:budget].decode("utf-8", errors="ignore") + note
    return body


def github_request(
    method: str, api_url: str, endpoint: str, token: str, payload: dict[str, Any] | None = None
) -> Any:
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    request = Request(
        f"{api_url.rstrip('/')}{endpoint}",
        data=data,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "pr-quality-reporter",
            "X-GitHub-Api-Version": "2022-11-28",
        },
        method=method,
    )
    try:
        with urlopen(request, timeout=30) as response:
            return json.load(response)
    except HTTPError as error:
        details = error.read().decode("utf-8", errors="replace")[:1000]
        raise RuntimeError(f"GitHub API {method} {endpoint} returned HTTP {error.code}: {details}") from error
    except URLError as error:
        raise RuntimeError(f"GitHub API {method} {endpoint} could not be reached: {error.reason}") from error


def find_existing_comment(api_url: str, repository: str, pr_number: int, token: str) -> int | None:
    page = 1
    while True:
        query = urlencode({"per_page": 100, "page": page})
        comments = github_request(
            "GET", api_url, f"/repos/{repository}/issues/{pr_number}/comments?{query}", token
        )
        for comment in comments:
            if (
                (comment.get("user") or {}).get("login") == BOT_LOGIN
                and (comment.get("body") or "").startswith(MARKER)
            ):
                return int(comment["id"])
        if len(comments) < 100:
            return None
        page += 1


def publish() -> None:
    token = required_env("GITHUB_TOKEN")
    repository = required_env("GITHUB_REPOSITORY")
    pr_number = int(required_env("PR_NUMBER"))
    if pr_number <= 0:
        raise ValueError("PR_NUMBER must be positive")
    api_url = required_env("GITHUB_API_URL")
    server_url = required_env("GITHUB_SERVER_URL").rstrip("/")
    run_id = required_env("GITHUB_RUN_ID")
    verify = required_env("VERIFY_RESULT")
    mutation = required_env("MUTATION_RESULT")

    run_url = f"{server_url}/{repository}/actions/runs/{run_id}"
    body = make_comment(run_url, verify, mutation)
    existing = find_existing_comment(api_url, repository, pr_number, token)

    if existing is not None:
        github_request(
            "PATCH", api_url, f"/repos/{repository}/issues/comments/{existing}", token, {"body": body}
        )
        print(f"Updated PR #{pr_number} comment {existing}")
    else:
        github_request(
            "POST", api_url, f"/repos/{repository}/issues/{pr_number}/comments", token, {"body": body}
        )
        print(f"Created PR #{pr_number} quality comment")


if __name__ == "__main__":
    try:
        publish()
    except (ValueError, RuntimeError, OSError) as error:
        print(f"::error::{error}", file=sys.stderr)
        raise SystemExit(1) from error
