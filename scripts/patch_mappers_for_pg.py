#!/usr/bin/env python3
"""Patch MyBatis mapper XML files for PostgreSQL."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

REPLACEMENTS = [
    ("sysdate()", "now()"),
    ("find_in_set(#{deptId}, ancestors)", "(',' || ancestors || ',') LIKE ('%,' || #{deptId} || ',%')"),
    ("date_format(create_time,'%Y%m%d')", "TO_CHAR(create_time, 'YYYYMMDD')"),
    ("date_format(#{params.beginTime},'%Y%m%d')", "TO_CHAR(#{params.beginTime}::timestamp, 'YYYYMMDD')"),
    ("date_format(#{params.endTime},'%Y%m%d')", "TO_CHAR(#{params.endTime}::timestamp, 'YYYYMMDD')"),
    ("ifnull(perms,'')", "COALESCE(perms,'')"),
    ("ifnull(m.perms,'')", "COALESCE(m.perms,'')"),
    ("`query`", '"query"'),
    ("insert ignore into sys_notice_read", "INSERT INTO sys_notice_read"),
    ("(select database())", "current_database()"),
]

NOTICE_READ_ON_CONFLICT = """INSERT INTO sys_notice_read (notice_id, user_id, read_time)
        values (#{noticeId}, #{userId}, now())
        ON CONFLICT (user_id, notice_id) DO NOTHING"""

NOTICE_READ_BATCH_ON_CONFLICT = """INSERT INTO sys_notice_read (notice_id, user_id, read_time)
        values
        <foreach collection="noticeIds" item="noticeId" separator=",">
            (#{noticeId}, #{userId}, now())
        </foreach>
        ON CONFLICT (user_id, notice_id) DO NOTHING"""


def patch_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    if "SysNoticeReadMapper" in str(path):
        text = text.replace(
            "INSERT INTO sys_notice_read (notice_id, user_id, read_time)\n        values (#{noticeId}, #{userId}, now())",
            NOTICE_READ_ON_CONFLICT,
        )
        if "ON CONFLICT" not in text.split("insertNoticeReadBatch")[1][:400]:
            text = text.replace(
                """INSERT INTO sys_notice_read (notice_id, user_id, read_time)
        values
        <foreach collection="noticeIds" item="noticeId" separator=",">
            (#{noticeId}, #{userId}, now())
        </foreach>""",
                NOTICE_READ_BATCH_ON_CONFLICT,
            )
    if text != original:
        path.write_text(text, encoding="utf-8")
        return True
    return False


def main():
    changed = 0
    for path in ROOT.rglob("*.xml"):
        if "mapper" in path.parts and patch_file(path):
            print(f"patched {path.relative_to(ROOT)}")
            changed += 1
    print(f"done, {changed} files changed")


if __name__ == "__main__":
    main()
