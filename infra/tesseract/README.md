# Tesseract OCR 语言包（tessdata）

知库通过 **Tess4J** 在 JVM 内调用 OCR，Windows 下原生 DLL 已随 `tess4j` Maven 依赖提供，**无需单独安装 Tesseract 安装包**。

本目录用于存放 **语言包**（`.traineddata`），默认需：

| 文件 | 用途 |
|------|------|
| `chi_sim.traineddata` | 简体中文 |
| `eng.traineddata` | 英文（与中文组合 `chi_sim+eng`） |

## 一键下载

```powershell
# 仓库根目录
.\scripts\setup-tesseract.ps1
```

Linux / macOS：

```bash
./scripts/setup-tesseract.sh
```

## 启用 OCR

1. 执行上述脚本下载语言包  
2. 在 `knowbase-service/src/main/resources/application.yml` 中设置：

   ```yaml
   ingest:
     ocr:
       enabled: true
       data-path: ./infra/tesseract/tessdata
   ```

3. 重启 Java 服务；在知识库向导中开启 **OCR** 开关  

Windows 还需安装 [Visual C++ Redistributable](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist)。

## 说明

- `.traineddata` 体积较大，已加入 `.gitignore`，不提交到 Git  
- Linux 生产环境建议在 Docker 镜像中 `apt install tesseract-ocr tesseract-ocr-chi-sim`，无需本目录
