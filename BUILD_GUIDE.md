# FameCode 客户端跨平台打包指南 (macOS & Windows)

本文档详细介绍如何将 **FameCode** 桌面客户端（基于 Tauri v2 + React + Rust）分别打包为 **macOS（DMG / APP）** 和 **Windows（EXE / MSI）** 平台的独立桌面安装包。

---

## 📑 目录

- [一、macOS 平台打包指南](#一macos-平台打包指南)
  - [1. 依赖与环境准备](#1-依赖与环境准备)
  - [2. 准备依赖与配置](#2-准备依赖与配置)
  - [3. 执行打包命令 (ARM64 / Intel / Universal)](#3-执行打包命令-arm64--intel--universal)
  - [4. 打包产物路径](#4-打包产物路径)
  - [5. 常见问题：解决 macOS Gatekeeper 拦截](#5-常见问题解决-macos-gatekeeper-拦截)
- [二、Windows 平台打包指南](#二windows-平台打包指南)
  - [1. 为什么不能直接在 macOS 交叉编译 Windows](#1-为什么不能直接在-macos-交叉编译-windows)
  - [2. Windows 本地编译环境搭建](#2-windows-本地编译环境搭建)
  - [3. 执行打包命令 (EXE / MSI)](#3-执行打包命令-exe--msi)
  - [4. 打包产物路径](#4-打包产物路径-1)
  - [5. 常见问题：Windows SmartScreen 拦截](#5-常见问题windows-smartscreen-拦截)
- [三、CI/CD 终极方案：GitHub Actions 自动化多平台构建](#三cicd-终极方案github-actions-自动化多平台构建)

---

## 一、macOS 平台打包指南

### 1. 依赖与环境准备

在 macOS 终端中确认以下工具链已就绪：

```bash
# 1. 检查 Rust 编译器与包管理器
rustc -V
cargo -V

# 2. 检查 Xcode Command Line Tools（提供制作 dmg 所需的 hdiutil 等系统工具）
xcode-select -p
# 若未安装请运行: xcode-select --install

# 3. 检查 Node.js 与 npm
node -v
npm -v
```

添加对应的 Rust 目标平台架构（按需添加）：
```bash
# Apple Silicon (M1/M2/M3/M4 系列芯片)
rustup target add aarch64-apple-darwin

# Intel 芯片 (x86_64)
rustup target add x86_64-apple-darwin
```

---

### 2. 准备依赖与配置

进入客户端工程目录并确保前端依赖安装完成：

```bash
cd client
npm install
```

客户端配置文件位于 `client/src-tauri/tauri.conf.json`，关键配置项如下：
```json
{
  "productName": "FameCode",
  "version": "0.1.0",
  "identifier": "cn.famecode.ai",
  "build": {
    "beforeBuildCommand": "npm run build",
    "frontendDist": "../dist"
  },
  "bundle": {
    "active": true,
    "targets": "all"
  }
}
```

---

### 3. 执行打包命令 (ARM64 / Intel / Universal)

在 `client` 目录下执行打包：

#### 方案 A：打包 Mac ARM 架构（Apple Silicon 推荐）
```bash
npm run tauri build -- --target aarch64-apple-darwin --bundles dmg --no-sign
```

#### 方案 B：打包 Mac Intel 架构 (x86_64)
```bash
npm run tauri build -- --target x86_64-apple-darwin --bundles dmg --no-sign
```

#### 方案 C：打包 Universal 通用架构（一个 DMG 兼容所有 Mac）
```bash
# 需要同时安装两端 target
rustup target add aarch64-apple-darwin x86_64-apple-darwin

# 构建 Universal 包
npm run tauri build -- --target universal-apple-darwin --bundles dmg --no-sign
```

> **参数说明**：
> - `--target`：指定编译架构。
> - `--bundles dmg`：指定仅输出 `.dmg`（若去掉该参数，会同时生成 `.app` 和 `.dmg`）。
> - `--no-sign`：跳过商业证书签名（内测、个人使用必加，防止因缺少 Apple Developer 开发者账号报错）。

---

### 4. 打包产物路径

构建完成后，产物输出在：

| 架构目标 | DMG 产物绝对路径 |
| :--- | :--- |
| **ARM64 (Apple Silicon)** | `client/src-tauri/target/aarch64-apple-darwin/release/bundle/dmg/FameCode_0.1.0_aarch64.dmg` |
| **Intel (x86_64)** | `client/src-tauri/target/x86_64-apple-darwin/release/bundle/dmg/FameCode_0.1.0_x64.dmg` |
| **Universal (通用)** | `client/src-tauri/target/universal-apple-darwin/release/bundle/dmg/FameCode_0.1.0_universal.dmg` |

同时在对应架构目录下的 `bundle/macos/` 中会生成 `FameCode.app` 原生应用。

---

### 5. 常见问题：解决 macOS Gatekeeper 拦截

未签名的应用在其他 Mac 上打开时可能会出现以下提示：
- *“无法打开 FameCode，因为无法验证开发者”*
- *“FameCode 已损坏，您应该将它移到废纸篓”*

**解决方案（在目标 Mac 终端执行一条命令）**：
```bash
xattr -cr /Applications/FameCode.app
```
清除隔离扩展属性后，即可正常双击启动。

---

## 二、Windows 平台打包指南

### 1. 为什么不能直接在 macOS 交叉编译 Windows

Tauri 客户端在 Windows 上依赖底层的 **Microsoft Edge WebView2** 渲染引擎和 **Windows C++ MSVC** 运行库链接。
目前官方并不推荐在 macOS/Linux 上直接交叉编译 Windows 二进制（极难配置完整的 MSVC CRT 和 Windows SDK），最佳实践为：
- **在 Windows 物理机 / 虚拟机中编译**；或者
- **使用 GitHub Actions 云端 Windows 虚拟机自动编译（最推荐）**。

---

### 2. Windows 本地编译环境搭建

在 Windows 10/11 电脑上准备以下环境：

1. **安装 C++ 生成工具（Build Tools）**：
   - 下载并安装 [Visual Studio 2022 Community](https://visualstudio.microsoft.com/zh-hans/downloads/) 或 [Visual Studio 生成工具](https://visualstudio.microsoft.com/zh-hans/visual-cpp-build-tools/)。
   - 安装时勾选：**“使用 C++ 的桌面开发”**（Desktop development with C++）。

2. **安装 Rust 工具链**：
   - 访问 [https://rustup.rs/](https://rustup.rs/) 下载并运行 `rustup-init.exe`。
   - 选择默认的 `x86_64-pc-windows-msvc` 工具链。

3. **安装 Node.js**：
   - 下载并安装 Node.js LTS (v18+)：[https://nodejs.org/](https://nodejs.org/)。

4. **确认 WebView2 Runtime**：
   - Windows 10 (较新版) 和 Windows 11 已内置 WebView2；若为旧系统可从微软官网下载安装 Evergreen 独立安装包。

5. **安装 WiX Toolset / NSIS（安装包制作工具）**：
   - Tauri v2 默认推荐使用 **NSIS** 生成轻量快速的 `.exe` 安装程序（Tauri CLI 会在首次构建时自动下载 NSIS，无需额外手动配置）。
   - 若需要 `.msi` 格式，可安装 [WiX Toolset v3](https://wixtoolset.org/)。

---

### 3. 执行打包命令 (EXE / MSI)

在 Windows 终端（PowerShell 或 CMD）中进入项目并执行：

```powershell
# 1. 进入 client 目录
cd fameCoding\client

# 2. 安装前端依赖
npm install

# 3. 打包生成 Windows 安装包（默认生成 NSIS .exe 安装包）
npm run tauri build

# 4. 若需显式指定打包格式：
# 生成 NSIS .exe 安装包
npm run tauri build -- --bundles nsis

# 生成 MSI 安装包
npm run tauri build -- --bundles msi
```

---

### 4. 打包产物路径

构建完成后，安装包将输出在以下位置：

| 安装包类型 | 文件路径 |
| :--- | :--- |
| **NSIS 安装包 (.exe)** | `client\src-tauri\target\release\bundle\nsis\FameCode_0.1.0_x64-setup.exe` |
| **MSI 安装包 (.msi)** | `client\src-tauri\target\release\bundle\msi\FameCode_0.1.0_x64_en-US.msi` |

双击运行即可在 Windows 电脑上完成安装。

---

### 5. 常见问题：Windows SmartScreen 拦截

在未经过微软商业数字证书签名时，Windows Defender SmartScreen 可能会提示：
- *“Windows 已保护你的电脑（未知发布者）”*

**解决方案**：
点击弹窗中的 **“更多信息”** ➔ 点击 **“仍要运行”** 即可。

---

## 三、CI/CD 终极方案：GitHub Actions 自动化多平台构建

由于跨平台打包需要不同的操作系统环境，强烈推荐利用 GitHub 提供的免费 CI/CD 运行器，在代码推送到远程仓库或发布 Release Tag 时**同时自动打包 macOS (ARM64/x64) 和 Windows (x64)**，并自动生成下载附件。

在工程根目录创建工作流文件 `.github/workflows/release.yml`：

```yaml
name: "Release Desktop App"

on:
  push:
    tags:
      - 'v*' # 只要推送例如 v0.1.0 标签即可触发自动打包
  workflow_dispatch: # 支持在 GitHub 网页手动点击触发

jobs:
  build-tauri:
    permissions:
      contents: write
    strategy:
      fail-fast: false
      matrix:
        include:
          # macOS Apple Silicon (ARM64)
          - platform: 'macos-latest'
            args: '--target aarch64-apple-darwin --bundles dmg --no-sign'
          # Windows (x64)
          - platform: 'windows-latest'
            args: '--bundles nsis'

    runs-on: ${{ matrix.platform }}
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
          cache-dependency-path: client/package-lock.json

      - name: Setup Rust
        uses: dtolnay/rust-toolchain@stable
        with:
          targets: ${{ matrix.platform == 'macos-latest' && 'aarch64-apple-darwin' || '' }}

      - name: Install Frontend Dependencies
        run: |
          cd client
          npm install

      - name: Build Desktop Application (Tauri)
        uses: tauri-apps/tauri-action@v0
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          projectPath: './client'
          tagName: ${{ github.ref_name }}
          releaseName: 'FameCode ${{ github.ref_name }}'
          releaseBody: 'See the assets to download FameCode for macOS and Windows.'
          releaseDraft: false
          prerelease: false
          args: ${{ matrix.args }}
```

### 使用方式：
1. 提交并推送该 workflow 文件到 GitHub 仓库；
2. 当需要发布新版本时，打上一个版本 tag 并推送：
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```
3. GitHub Actions 会自动启动两台云主机（macOS 和 Windows），几分钟后便会在 GitHub Releases 页面提供下载：
   - 🍏 `FameCode_0.1.0_aarch64.dmg` (Mac ARM 版)
   - 🪟 `FameCode_0.1.0_x64-setup.exe` (Windows 版)
