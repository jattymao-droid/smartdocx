# 组卷网试题采集工具

从 [组卷网](https://zujuan.xkw.com/) 采集题目，导入 RuoYi-Cloud 题库 API。

## 开发模式启动

```powershell
cd D:\AI\DlEducation\RuoYi-Cloud
pip install -r zujuan_collector\requirements.txt
python -m playwright install chromium
# 若需识别答案图片，先启动 OCR（另开终端）
.\scripts\start-ocr-service.ps1
.\scripts\start-zujuan-collector.ps1
```

## 配置

保存在 `%APPDATA%\SchoolManagement\ZujuanCollector\config.json`：

- `api_base`: `http://localhost:8080`（网关）
- `redis_cli`: 验证码开启时必填，如 `C:\Program Files\Redis\redis-cli.exe`
- `subject_id`: 导入学科 ID

## 打包 EXE

```powershell
.\packages\question-bank\collector\build_desktop.ps1
```

## CLI

```bash
python -m zujuan_collector.collector login
python -m zujuan_collector.collector collect --url https://zujuan.xkw.com/gzwl/zj136248/ --pages 1 -o data/out.json
python -m zujuan_collector.collector import --file data/out.json
```

## 注意事项

- 采集依赖 Playwright Chromium，需先在桌面端完成组卷网登录
- 勾选「获取答案解析」时优先调用组卷网 API，失败时回退 OCR
- 导入前自动校验题型/选项/知识点，并从解析回填各题型答案
- 默认跳过重复题目（同批次 zujuan_id 或题库内容重复）
- 导入时章节无法匹配会自动创建（`auto_create_chapters`，需 `education:textbook:add` 权限）
- 导入账号需具备 `education:question:add` 权限
- 仅在授权范围内使用

## 测试

```powershell
python zujuan_collector\test_collector_accuracy.py
```
