#!/usr/bin/env python3
"""Usage:
   python3 ai_analyze.py log   <console_log_file>
   python3 ai_analyze.py trivy <report1.txt> [report2.txt ...]
AI step kabhi pipeline fail nahi karta (hamesha exit 0)."""
import json, os, re, sys, time, urllib.error, urllib.request

MODE, FILES = sys.argv[1], sys.argv[2:]
KEY = os.getenv("GEMINI_API_KEY")
MODELS = [m for m in (os.getenv("GEMINI_MODEL"), os.getenv("GEMINI_FALLBACK_MODEL")) if m]
MAX_CHARS = 6000


def mask(text):
    text = re.sub(r"AKIA[0-9A-Z]{16}", "[AWS_KEY]", text)
    text = re.sub(r"AIza[0-9A-Za-z_\-]{20,}", "[API_KEY]", text)
    text = re.sub(r"(?i)(password|passwd|token|secret|api[_-]?key)\s*[=:]\s*\S+", r"\1=[MASKED]", text)
    text = re.sub(r"(?i)bearer\s+\S+", "Bearer [MASKED]", text)
    return text


def clean_log(text):
    text = re.sub(r"\x1b\[8m.*?\x1b\[0m", "", text)   # Jenkins console notes
    return re.sub(r"\x1b\[[0-9;]*m", "", text)        # ANSI colors


def load():
    if MODE == "log":
        lines = clean_log(open(FILES[0], errors="ignore").read()).splitlines()[-150:]
        return "\n".join(lines), (
            "You are a senior DevOps engineer. Below is the tail of a failed Jenkins build log "
            "(Spring Boot microservices, Maven, SonarQube, Trivy, Nexus, Docker, EC2). "
            "Give: 1) the failing stage, 2) the most likely root cause, 3) top 3 fixes in order. "
            "Be short. Use bullet points. If unsure, say so.")
    out = []
    for f in FILES:
        if not os.path.exists(f):
            continue
        rows = [l.strip() for l in open(f, errors="ignore")
                if ("CRITICAL" in l or "HIGH" in l) and ("CVE-" in l or "GHSA-" in l)]
        out.append(f"## {f}\n" + "\n".join(rows[:40]))
    return "\n\n".join(out), (
        "You are a DevSecOps engineer. Below are HIGH/CRITICAL findings from Trivy scans of a "
        "Spring Boot project and its Docker images. Give a prioritized list of max 5 fixes: "
        "which package/library, the fixed version, and what to change (pom.xml version or base image). "
        "Short bullet points only. Do not invent CVEs that are not in the input.")


def call_gemini(prompt, content):
    body = json.dumps({
        "contents": [{"parts": [{"text": f"{prompt}\n\n---\n{content}"}]}],
        "generationConfig": {"maxOutputTokens": 800},
    }).encode()
    last_err = None
    for model in MODELS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
        for attempt in range(3):
            try:
                req = urllib.request.Request(url, data=body, headers={
                    "Content-Type": "application/json", "x-goog-api-key": KEY})
                with urllib.request.urlopen(req, timeout=30) as r:
                    data = json.load(r)
                parts = data["candidates"][0]["content"]["parts"]
                return "".join(p.get("text", "") for p in parts).strip()
            except urllib.error.HTTPError as e:
                last_err = f"{model}: HTTP {e.code}"
                if e.code in (429, 500, 503):
                    time.sleep(2 ** attempt * 3)    # 3s, 6s, 12s
                    continue
                break                                # 400/403/404: retry se fayda nahi
            except Exception as e:
                last_err = f"{model}: {e}"
                time.sleep(3)
    raise RuntimeError(last_err or "no model configured")


try:
    if not KEY or not MODELS:
        raise RuntimeError("GEMINI_API_KEY or GEMINI_MODEL not set")
    content, prompt = load()
    if not content.strip():
        raise RuntimeError("no input found")
    result = call_gemini(prompt, mask(content)[-MAX_CHARS:])
    out = f"## AI analysis ({MODE})\n\n{result}\n"
except Exception as e:
    out = f"AI analysis skipped: {e}\n"

print(out)
open(f"ai-report-{MODE}.md", "w").write(out)
sys.exit(0)
