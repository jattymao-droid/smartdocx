#!/usr/bin/env python3
"""Import Nacos configs from PostgreSQL ry_config into Nacos 3.x server."""
import subprocess
import urllib.parse
import urllib.request

PSQL = r"C:\Program Files\PostgreSQL\16\bin\psql.exe"
NACOS = "http://127.0.0.1:8848/nacos/v3/admin/cs/config"
PG_ENV = {"PGPASSWORD": "mm5621528"}
HEADERS = {"serverIdentity": "security"}


def fetch_configs():
    cmd = [
        PSQL,
        "-U",
        "postgres",
        "-h",
        "localhost",
        "-d",
        "ry_config",
        "-A",
        "-F",
        "\t",
        "-c",
        "COPY (SELECT data_id, group_id, content, COALESCE(type,'yaml') FROM config_info ORDER BY id) TO STDOUT",
    ]
    out = subprocess.check_output(cmd, env={**subprocess.os.environ, **PG_ENV}, text=True, encoding="utf-8")
    rows = []
    for line in out.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t", 3)
        if len(parts) < 4:
            continue
        rows.append((parts[0], parts[1], parts[2], parts[3]))
    return rows


def publish(data_id, group_id, content, typ):
    body = urllib.parse.urlencode(
        {
            "dataId": data_id,
            "groupName": group_id,
            "namespaceId": "public",
            "content": content,
            "type": typ,
        }
    ).encode("utf-8")
    req = urllib.request.Request(NACOS, data=body, method="POST", headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode()


def main():
    rows = fetch_configs()
    ok = 0
    for data_id, group_id, content, typ in rows:
        try:
            publish(data_id, group_id, content, typ)
            print(f"OK  {data_id}")
            ok += 1
        except Exception as e:
            print(f"FAIL {data_id}: {e}")
    print(f"Imported {ok}/{len(rows)} configs")


if __name__ == "__main__":
    main()
