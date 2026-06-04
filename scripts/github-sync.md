# 同步到 GitHub（ch-dpf）

本机已完成：`git init`、首次提交、`main` 分支、`origin` → `https://github.com/ch-dpf/doc-platform.git`。

自动推送失败原因：GitHub 上尚未创建该仓库，或当前环境未登录 GitHub 账号。

## 一、在 GitHub 创建空仓库

1. 打开 [https://github.com/new](https://github.com/new)（或你的仓库列表 [ch-dpf](https://github.com/ch-dpf?tab=repositories) → **New**）。
2. **Repository name**：`doc-platform`（须与下面远程地址一致）。
3. 选择 **Private** 或 **Public**。
4. **不要**勾选 “Add a README / .gitignore / license”（本地已有代码）。
5. 点击 **Create repository**。

## 二、本机推送（PowerShell）

```powershell
cd D:\workspace\doc-platform

# 若 commit 报错 unknown option trailer，请用完整路径调用 git：
# & "D:\software\git\Git\cmd\git.exe" commit -m "your message"

# 确认远程（已配置可跳过）
git remote -v
# 若无 origin：
# git remote add origin https://github.com/ch-dpf/doc-platform.git

# 推送（会弹出 GitHub 登录或要求 Personal Access Token）
git push -u origin main
```

### HTTPS + Personal Access Token（推荐）

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)** → **Generate new token**。
2. 勾选 **`repo`** 权限，生成后复制 token（只显示一次）。
3. 推送时：
   - **Username**：`ch-dpf`
   - **Password**：粘贴 **token**（不是 GitHub 登录密码）。

### SSH（可选）

```powershell
git remote set-url origin git@github.com:ch-dpf/doc-platform.git
git push -u origin main
```

需先在 GitHub 添加本机 SSH 公钥（Settings → SSH and GPG keys）。

## 三、后续更新

```powershell
cd D:\workspace\doc-platform
git add -A
git status
git commit -m "描述本次修改"
git push
```

## 四、安装 GitHub CLI（可选）

```powershell
winget install GitHub.cli
gh auth login
gh repo create ch-dpf/doc-platform --private --source=. --remote=origin --push
```

`--private` 可改为 `--public`。
