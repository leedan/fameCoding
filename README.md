# FameCode

FameCode 是一款企业级全栈 AI 桌面 IDE 与研发效能平台。

## 目录结构

```
fameCoding/
├── server/       # FameCode 后端服务 (Spring Boot 3 + Spring AI + Google ADK + MyBatis)
│   ├── famecode-server-api
│   ├── famecode-server-types
│   ├── famecode-server-domain
│   ├── famecode-server-case
│   ├── famecode-server-trigger
│   ├── famecode-server-infrastructure
│   └── famecode-server-app
└── client/       # FameCode 桌面客户端 (Tauri 2.0 + React 18 + TypeScript + Monaco Editor)
```

## 核心特性

- **全栈 AI 智能体 (FameCode Agent)**：集成本地代码读写、智能搜索、项目编译、单元测试、安全审查与本地命令执行。
- **混合动态路由**：支持端侧弱模型分类路由、内网大模型与商业大模型混合架构。
- **ReAct 监督循环**：工具调用安全护栏、循环深度监控、单轮工具并发防超载。
- **行内智能补全 (Inline Completion)**：Monaco Editor 毫秒级多行代码智能预测。

## 快速启动

### 1. 后端启动
```bash
cd server
mvn clean package -DskipTests
java -jar famecode-server-app/target/famecode-server-app.jar
```

### 2. 客户端启动
```bash
cd client
npm install
npm run tauri dev
```

## 客户端打包与发布

详细的 macOS（DMG / APP）与 Windows（EXE / MSI）客户端打包教程及 CI/CD 自动化构建配置，请参阅：
👉 **[客户端跨平台打包指南 (BUILD_GUIDE.md)](./BUILD_GUIDE.md)**
