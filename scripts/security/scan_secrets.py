#!/usr/bin/env python3
"""Look for secrets in the working tree, a diff, or the whole git history (S004, S005).

Prints where each finding is and what kind it is, never the value: a short fingerprint (a hash)
tells findings apart. Exit status 1 when anything that isn't public by design is found.

    python3 scripts/security/scan_secrets.py            # tracked files in the working tree
    python3 scripts/security/scan_secrets.py --history  # every commit on every branch
    python3 scripts/security/scan_secrets.py --staged   # what's about to be committed (pre-commit hook)

Public by design, reported only with --verbose: Supabase publishable (anon) keys and the local
stack's well-known demo keys, Firebase app settings, Google OAuth client ids.
"""
import base64
import hashlib
import json
import re
import subprocess
import sys

PATTERNS = [
    ("private key", re.compile(r"-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----")),
    ("Google API key", re.compile(r"AIza[0-9A-Za-z_\-]{35}")),
    ("Supabase secret key", re.compile(r"sb_secret_[0-9A-Za-z_\-]{20,}")),
    ("Supabase publishable key", re.compile(r"sb_publishable_[0-9A-Za-z_\-]{20,}")),
    ("JWT", re.compile(r"eyJ[0-9A-Za-z_\-]{10,}\.eyJ[0-9A-Za-z_\-]{10,}\.[0-9A-Za-z_\-]{10,}")),
    ("Supabase access token", re.compile(r"sbp_[0-9a-f]{40}")),
    ("GitHub token", re.compile(r"gh[pousr]_[0-9A-Za-z]{36,}")),
    ("AWS access key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("Slack token", re.compile(r"xox[baprs]-[0-9A-Za-z\-]{10,}")),
    ("Stripe live key", re.compile(r"sk_live_[0-9A-Za-z]{20,}")),
    ("OpenAI key", re.compile(r"sk-(?:proj-)?[0-9A-Za-z_\-]{32,}")),
    # A password written into a file: a quoted literal, or KEY=value in a properties or env file.
    ("password literal", re.compile(r"(?i)password\s*[=:]\s*[\"'][^\"'\s$]{6,}[\"']")),
    ("password property", re.compile(r"^\s*[A-Z_]*PASSWORD\s*=\s*[^\s$]{6,}\s*$")),
]

# Values that are documentation or placeholders, not secrets.
PLACEHOLDER = re.compile(r"(?i)placeholder|example|your[-_ ]|xxxx|changeme|local-test|not-a-real|<|\$\{|\$\(|secrets\.|getenv|env\(|buildSetting|\.\.\.")


def jwt_kind(token: str) -> str:
    """'service_role', 'anon', 'demo-<role>' (the local stack's public demo keys) or 'other'."""
    try:
        payload = token.split(".")[1]
        payload += "=" * (-len(payload) % 4)
        claims = json.loads(base64.urlsafe_b64decode(payload))
    except Exception:
        return "other"
    role = claims.get("role", "other")
    if claims.get("iss") == "supabase-demo":
        return f"demo-{role}"
    return role


def classify(kind: str, value: str):
    """(severity, label): severity is 'secret', 'public' or None to ignore."""
    if PLACEHOLDER.search(value):
        return None, kind
    if kind == "JWT":
        role = jwt_kind(value)
        if role.startswith("demo-"):
            return "public", f"Supabase local demo key ({role[5:]})"
        if role == "anon":
            return "public", "Supabase publishable (anon) key"
        if role == "service_role":
            return "secret", "Supabase SERVICE ROLE key"
        return "secret", f"JWT ({role})"
    if kind == "Supabase publishable key":
        return "public", kind
    return "secret", kind


def fingerprint(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()[:10]


def scan_text(text: str, where: str, findings: list, first_line: int = 1):
    for lineno, line in enumerate(text.splitlines(), first_line):
        for kind, rx in PATTERNS:
            for m in rx.finditer(line):
                # A key block built at run time (a test's throwaway key) has its body interpolated.
                if kind == "private key" and ("${" in line or "btoa(" in line):
                    continue
                severity, label = classify(kind, m.group(0))
                if severity:
                    findings.append((severity, label, f"{where}:{lineno}", fingerprint(m.group(0))))


def git(*args) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, encoding="utf-8", errors="replace").stdout


SKIP = re.compile(r"(^|/)(node_modules|build|\.gradle)/|\.(png|jpg|jpeg|webp|ttf|otf|jar|apk|aab|gz|zip|db)$|scan_secrets\.py$")


def scan_tree(findings):
    for path in git("ls-files").splitlines():
        if SKIP.search(path):
            continue
        try:
            with open(path, encoding="utf-8", errors="replace") as f:
                scan_text(f.read(), path, findings)
        except OSError:
            continue


def scan_patch(patch: str, findings, label_prefix=""):
    commit, path, lineno = "", "", 0
    for line in patch.splitlines():
        if line.startswith("commit "):
            commit = line.split()[1][:10]
        elif line.startswith("+++ b/"):
            path = line[6:]
        elif line.startswith("@@"):
            m = re.search(r"\+(\d+)", line)
            lineno = int(m.group(1)) - 1 if m else 0
        elif line.startswith("+") and not line.startswith("+++"):
            lineno += 1
            if path and not SKIP.search(path):
                where = f"{label_prefix}{commit + ' ' if commit else ''}{path}"
                scan_text(line[1:], where, findings, first_line=lineno)
        elif not line.startswith("-"):
            lineno += 1


def main():
    args = set(sys.argv[1:])
    findings = []
    if "--history" in args:
        scan_patch(git("log", "-p", "--all", "--no-color", "--unified=0", "--format=commit %H"), findings)
    elif "--staged" in args:
        scan_patch(git("diff", "--cached", "--no-color", "--unified=0"), findings)
    else:
        scan_tree(findings)
    secrets = [f for f in findings if f[0] == "secret"]
    public = [f for f in findings if f[0] == "public"]
    for severity, label, where, fp in sorted(set(secrets)):
        print(f"SECRET  {label}  at {where}  (fingerprint {fp})")
    if "--verbose" in args:
        for severity, label, where, fp in sorted(set(public)):
            print(f"public  {label}  at {where}  (fingerprint {fp})")
    print(f"{len(set(secrets))} secret finding(s); {len(set(public))} public-by-design value(s).")
    return 1 if secrets else 0


if __name__ == "__main__":
    sys.exit(main())
