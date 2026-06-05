# 同步到 GitHub（ch-dpf）

本机已完成：`git init`、首次提交、`main` 分支、`origin` → `https://github.com/ch-dpf/knowbase.git`。

自动推送失败原因：GitHub 上尚未创建该仓库，或当前环境未登录 GitHub 账号。

## 一、在 GitHub 创建空仓库

1. 打开 [https://github.com/new](https://github.com/new)（或你的仓库列表 [ch-dpf](https://github.com/ch-dpf?tab=repositories) → **New**）。
2. **Repository name**：`knowbase`（须与下面远程地址一致）。
3. 选择 **Private** 或 **Public**。
4. **不要**勾选 “Add a README / .gitignore / license”（本地已有代码）。
5. 点击 **Create repository**。

## 二、本机推送（PowerShell）

```powershell
cd <仓库根目录>

# 若 commit 报错 unknown option trailer，请用完整路径调用 git：
# & "D:\software\git\Git\cmd\git.exe" commit -m "your message"

# 确认远程（已配置可跳过）
git remote -v
# 若无 origin：
# git remote add origin https://github.com/ch-dpf/knowbase.git

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

**若出现 `Permission denied (publickey)`**：说明本机还没有密钥或未添加到 GitHub。

```powershell
# 1. 生成密钥（一路回车即可，也可自设密码）
ssh-keygen -t ed25519 -C "your_email@example.com" -f $env:USERPROFILE\.ssh\id_ed25519

# 2. 查看公钥，复制整行
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub

# 3. GitHub → Settings → SSH and GPG keys → New SSH key → 粘贴公钥 → Save

# 4. 测试
ssh -T git@github.com

# 5. 推送
cd <仓库根目录>
git remote set-url origin git@github.com:ch-dpf/knowbase.git
git push -u origin main
```

### 常见错误

| 报错 | 处理 |
|------|------|
| `Permission denied (publickey)` | 按上文生成密钥并添加到 GitHub，或改用 HTTPS |
| `Repository not found` | 先在 GitHub 创建空仓库 `knowbase`，或检查账号/仓库名 |
| `OpenSSL SSL_read: Connection was reset` (errno 10054) | 见下文 **「HTTPS 连接被重置」** |
| `Failed to connect ... Timed out` | 检查代理/VPN，或为 Git 配置 `http.proxy` |
| `Are you sure you want to continue connecting` | 输入 **yes**（首次连接正常） |

### HTTPS 连接被重置（浏览器能开 GitHub，git push 失败）

浏览器往往走**系统代理**，Git 默认**不走代理**，且旧版 Git 使用 OpenSSL，容易被中断。

**按顺序尝试：**

```powershell
# 1) 让 Git 使用 Windows 系统证书栈（常能缓解 SSL 问题）
git config --global http.sslBackend schannel

# 2) 若本机系统代理为 127.0.0.1:7890（Clash 等常见端口）：
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
# 本仓库环境已配置上述三项 + http.sslBackend=schannel

# 3) 可选：强制 HTTP/1.1
git config --global http.version HTTP/1.1

# 4) 再推送
cd <仓库根目录>
git push -u origin main
```

取消代理（不需要时）：

```powershell
git config --global --unset http.proxy
git config --global --unset https.proxy
```

**仍失败时：**

- 升级 [Git for Windows](https://git-scm.com/download/win)（建议 ≥ 2.43）
- 在代理工具中开启 **TUN/系统代理**，或允许 `git.exe` 走代理
- 改用 **SSH**（配置好公钥后有时比 HTTPS 稳定）：`git remote set-url origin git@github.com:ch-dpf/knowbase.git`
- 临时方案：用 [GitHub Desktop](https://desktop.github.com/) 打开本目录推送（会跟随系统代理）

## 三、后续更新

```powershell
cd <仓库根目录>
git add -A
git status
git commit -m "描述本次修改"
git push
```

## 四、安装 GitHub CLI（可选）

```powershell
winget install GitHub.cli
gh auth login
gh repo create ch-dpf/knowbase --private --source=. --remote=origin --push
```

`--private` 可改为 `--public`。
