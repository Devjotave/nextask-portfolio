"""Check the production stack through its public frontend port."""

import json
import os
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen


BASE_URL = os.environ.get("SMOKE_BASE_URL", "http://127.0.0.1:8080")


def request(path, expected, payload=None, token=None):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    req = Request(BASE_URL + path, data=data, headers=headers)
    try:
        response = urlopen(req, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        body = response.read().decode()
        if response.status != expected:
            raise AssertionError(f"{path}: expected {expected}, got {response.status}")
        return body, response.headers


def main():
    body, _ = request("/health", 200)
    assert body.strip() == "ok"
    index, headers = request("/", 200)
    assert "text/html" in headers.get("Content-Type", "")
    route, _ = request("/workspaces", 200)
    assert route == index, "SPA route must serve index.html"

    for token in (None, "invalid.jwt.token"):
        body, headers = request("/api/users/me", 401, token=token)
        assert "application/problem+json" in headers.get("Content-Type", "")
        assert json.loads(body)["status"] == 401

    password = "CI-only-smoke-password-123!"
    email = f"smoke-{uuid.uuid4()}@example.com"
    body, _ = request("/api/auth/register", 201, {
        "name": "Container smoke test", "email": email, "password": password,
    })
    registration = json.loads(body)
    assert registration["tokenType"] == "Bearer"
    body, _ = request("/api/users/me", 200, token=registration["accessToken"])
    user = json.loads(body)
    assert user["email"] == email
    assert "password" not in user and "passwordHash" not in user

    body, _ = request("/api/auth/login", 200, {"email": email, "password": password})
    login = json.loads(body)
    request("/api/users/me", 200, token=login["accessToken"])
    request("/api/auth/login", 401, {"email": email, "password": "wrong-password"})
    print("Container smoke checks passed: SPA, API proxy, authentication and SQL Server.")


if __name__ == "__main__":
    main()
