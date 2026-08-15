#!/usr/bin/env python3
"""Integrate question-bank package into RuoYi-Cloud."""

from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
_EXTRACTED = ROOT / "packages" / "question-bank" / "_extracted"
PKG = next(p for p in _EXTRACTED.iterdir() if p.is_dir()) if _EXTRACTED.is_dir() else None
SYSTEM = ROOT / "ruoyi-modules" / "ruoyi-system"
UI = ROOT / "ruoyi-ui"

JAVA_REPLACEMENTS = [
    ("com.ruoyi.common.core.controller.BaseController", "com.ruoyi.common.core.web.controller.BaseController"),
    ("com.ruoyi.common.core.domain.AjaxResult", "com.ruoyi.common.core.web.domain.AjaxResult"),
    ("com.ruoyi.common.core.page.TableDataInfo", "com.ruoyi.common.core.web.page.TableDataInfo"),
    ("com.ruoyi.common.core.domain.BaseEntity", "com.ruoyi.common.core.web.domain.BaseEntity"),
    ("com.ruoyi.common.exception.ServiceException", "com.ruoyi.common.core.exception.ServiceException"),
    ("com.ruoyi.common.utils.StringUtils", "com.ruoyi.common.core.utils.StringUtils"),
    ("com.ruoyi.common.utils.DateUtils", "com.ruoyi.common.core.utils.DateUtils"),
    ("com.ruoyi.common.utils.SecurityUtils", "com.ruoyi.common.security.utils.SecurityUtils"),
    ("com.ruoyi.common.utils.file.ImageUtils", "com.ruoyi.common.core.utils.file.ImageUtils"),
    ("com.ruoyi.common.utils.file.FileUtils", "com.ruoyi.common.core.utils.file.FileUtils"),
    ("com.ruoyi.common.utils.file.FileUploadUtils", "com.ruoyi.system.service.education.support.EduQbFileUploadUtils"),
    ("com.ruoyi.common.utils.uuid.Seq", "com.ruoyi.common.core.utils.uuid.Seq"),
    ("com.ruoyi.common.utils.http.HttpUtils", "com.ruoyi.system.service.education.support.EduQbHttpUtils"),
    ("com.ruoyi.common.config.RuoYiConfig", "com.ruoyi.system.service.education.support.EduQbLocalFileSupport"),
    ("com.ruoyi.common.config.EduQbOcrProperties", "com.ruoyi.system.config.EduQbOcrProperties"),
    ("com.ruoyi.common.config.EduQbDedupProperties", "com.ruoyi.system.config.EduQbDedupProperties"),
    ("com.ruoyi.common.config.EduQbAuditProperties", "com.ruoyi.system.config.EduQbAuditProperties"),
    ("package com.ruoyi.web.controller.education", "package com.ruoyi.system.controller.education"),
    ("package com.ruoyi.web.controller.wx.teacher", "package com.ruoyi.system.controller.wx"),
]

CONTROLLER_PKG = "com.ruoyi.system.controller.education"


def patch_java(content: str) -> str:
    for old, new in JAVA_REPLACEMENTS:
        content = content.replace(old, new)
    # Cloud BaseController has no getUsername()
    if "getUsername()" in content and "extends BaseController" in content:
        if "SecurityUtils" not in content:
            content = content.replace(
                "import org.springframework.web.bind.annotation.RestController;",
                "import org.springframework.web.bind.annotation.RestController;\n"
                "import com.ruoyi.common.security.utils.SecurityUtils;",
            )
        content = content.replace("getUsername()", "SecurityUtils.getUsername()")
    content = content.replace("FileUtils.stripPrefix(", "EduQbLocalFileSupport.stripPrefix(")
    return content


def copy_tree(src: Path, dst: Path, *, patch: bool = False) -> None:
    if not src.exists():
        print(f"skip missing: {src}")
        return
    if src.is_file():
        dst.parent.mkdir(parents=True, exist_ok=True)
        text = src.read_text(encoding="utf-8")
        if patch and src.suffix == ".java":
            text = patch_java(text)
        dst.write_text(text, encoding="utf-8")
        return
    for item in src.rglob("*"):
        if item.is_dir():
            continue
        rel = item.relative_to(src)
        target = dst / rel
        text = item.read_text(encoding="utf-8")
        if patch and item.suffix == ".java":
            text = patch_java(text)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")


def copy_frontend_api_and_views() -> None:
    fe = PKG / "frontend" / "ruoyi-ui" / "src"
    mappings = [
        (fe / "api" / "education", UI / "src" / "api" / "education"),
        (fe / "views" / "education", UI / "src" / "views" / "education"),
        (fe / "components" / "QbFormulaText", UI / "src" / "components" / "QbFormulaText"),
        (fe / "mixins" / "dynamicQuestionTypes.js", UI / "src" / "mixins" / "dynamicQuestionTypes.js"),
        (fe / "store" / "modules" / "questionBasket.js", UI / "src" / "store" / "modules" / "questionBasket.js"),
    ]
    utils = [
        "questionBasketFly.js",
        "questionBasketPrefs.js",
        "paperExportClient.js",
        "questionContent.js",
        "paperExportPdf.js",
        "paperExport.js",
        "questionFormula.js",
        "questionTypes.js",
        "paperExportDocx.js",
        "paperAnswerArea.js",
        "mathliveLocale.js",
    ]
    for name in utils:
        src = fe / "utils" / name
        if src.exists():
            mappings.append((src, UI / "src" / "utils" / name))

    for src, dst in mappings:
        if src.is_dir():
            if dst.exists():
                shutil.rmtree(dst)
            shutil.copytree(src, dst)
        else:
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)

    for js in (UI / "src" / "api" / "education").rglob("*.js"):
        text = js.read_text(encoding="utf-8")
        text = text.replace("url: '/education/", "url: '/system/education/")
        js.write_text(text, encoding="utf-8")


def main() -> None:
    if PKG is None or not PKG.exists():
        raise SystemExit(f"Package not found under: {_EXTRACTED}")

    # backend: system module (domain, mapper, service, resources)
    copy_tree(
        PKG / "backend" / "ruoyi-system" / "src" / "main" / "java",
        SYSTEM / "src" / "main" / "java",
        patch=True,
    )
    copy_tree(
        PKG / "backend" / "ruoyi-system" / "src" / "main" / "resources" / "mapper" / "education",
        SYSTEM / "src" / "main" / "resources" / "mapper" / "education",
    )

    # controllers
    admin_ctrl = PKG / "backend" / "ruoyi-admin" / "src" / "main" / "java" / "com" / "ruoyi" / "web" / "controller"
    for sub in ("education", "wx/teacher"):
        src = admin_ctrl / sub
        if not src.exists():
            continue
        dst_name = "education" if sub == "education" else "wx"
        dst = SYSTEM / "src" / "main" / "java" / "com" / "ruoyi" / "system" / "controller" / dst_name
        copy_tree(src, dst, patch=True)

    # config properties from package ruoyi-common
    cfg_src = PKG / "backend" / "ruoyi-common" / "src" / "main" / "java" / "com" / "ruoyi" / "common" / "config"
    cfg_dst = SYSTEM / "src" / "main" / "java" / "com" / "ruoyi" / "system" / "config"
    cfg_dst.mkdir(parents=True, exist_ok=True)
    for name in ("EduQbOcrProperties.java", "EduQbDedupProperties.java", "EduQbAuditProperties.java"):
        src = cfg_src / name
        if src.exists():
            text = patch_java(src.read_text(encoding="utf-8"))
            text = text.replace("package com.ruoyi.common.config;", "package com.ruoyi.system.config;")
            (cfg_dst / name).write_text(text, encoding="utf-8")

    copy_frontend_api_and_views()

    # SQL scripts to sql/postgresql/question_bank
    sql_dst = ROOT / "sql" / "postgresql" / "question_bank"
    sql_src = PKG / "database"
    if sql_dst.exists():
        shutil.rmtree(sql_dst)
    shutil.copytree(sql_src, sql_dst)

    print("Question bank integration copy completed.")


if __name__ == "__main__":
    main()
