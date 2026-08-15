import re
from pathlib import Path

ctrl = Path(r"D:/AI/DlEducation/RuoYi-Cloud/ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/controller")
pattern = re.compile(r'@PreAuthorize\("@ss\.hasPermi\(\'([^\']+)\'\)"\)')
for f in ctrl.rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    if "PreAuthorize" not in t:
        continue
    t = t.replace(
        "import org.springframework.security.access.prepost.PreAuthorize;",
        "import com.ruoyi.common.security.annotation.RequiresPermissions;",
    )
    t = pattern.sub(r'@RequiresPermissions("\1")', t)
    f.write_text(t, encoding="utf-8")
    print("perms", f.name)
