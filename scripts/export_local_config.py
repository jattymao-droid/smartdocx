#!/usr/bin/env python3
"""Export Nacos YAML configs from PostgreSQL to local config directory.

Prefer scripts/publish_local_config.py to push config/*.yml back to Nacos.
"""import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "config"
PSQL = r"C:\Program Files\PostgreSQL\16\bin\psql.exe"
ENV = {**subprocess.os.environ, "PGPASSWORD": "mm5621528"}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rows = subprocess.check_output(
        [
            PSQL,
            "-U",
            "postgres",
            "-h",
            "localhost",
            "-d",
            "ry_config",
            "-t",
            "-A",
            "-c",
            "SELECT data_id || '|||' || replace(content, E'\\n', '\\\\n') FROM config_info ORDER BY id",
        ],
        env=ENV,
        text=True,
        encoding="utf-8",
    )
    for line in rows.splitlines():
        if "|||" not in line:
            continue
        data_id, content = line.split("|||", 1)
        content = content.replace("\\n", "\n")
        (OUT / data_id).write_text(content, encoding="utf-8", newline="\n")
        print(f"exported {data_id}")


if __name__ == "__main__":
    main()
