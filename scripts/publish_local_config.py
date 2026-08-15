#!/usr/bin/env python3
"""Publish YAML configs from config/ directory to Nacos 3.x (fixes corrupted derby cache)."""
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG_DIR = ROOT / "config"
NACOS = "http://127.0.0.1:8848/nacos/v3/admin/cs/config"
HEADERS = {"serverIdentity": "security"}


def publish(data_id: str, content: str, typ: str = "yaml") -> None:
    body = urllib.parse.urlencode(
        {
            "dataId": data_id,
            "groupName": "DEFAULT_GROUP",
            "namespaceId": "public",
            "content": content,
            "type": typ,
        }
    ).encode("utf-8")
    req = urllib.request.Request(NACOS, data=body, method="POST", headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as resp:
        text = resp.read().decode("utf-8", errors="replace")
        if '"code":0' not in text and '"code": 0' not in text:
            raise RuntimeError(text)


def main():
    if not CONFIG_DIR.is_dir():
        raise SystemExit(f"config dir not found: {CONFIG_DIR}")
    files = sorted(CONFIG_DIR.glob("*.yml"))
    if not files:
        raise SystemExit("no *.yml in config/")
    ok = 0
    for path in files:
        data_id = path.name
        content = path.read_text(encoding="utf-8")
        try:
            publish(data_id, content)
            print(f"OK  {data_id}")
            ok += 1
        except Exception as ex:
            print(f"FAIL {data_id}: {ex}")
    print(f"Published {ok}/{len(files)} configs to Nacos")


if __name__ == "__main__":
    main()
