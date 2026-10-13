# 基于AI大模型的智能学习系统

前后端分离架构的智能学习平台,融合**智能生成题目**、**智能刷题**、**智能考试**三大核心场景:
基于知识点与难度要求调用大模型(本地 Ollama 部署 DeepSeek)自动出题,并用 AI 对主观题自动评分、生成学习建议;
用 Redis 缓存热门题目与题库分类等高频查询;用 MinIO 统一管理视频、图片等学习资源。

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + JWT(jjwt 0.12) + springdoc-openapi |
| 数据库 | MySQL 8.0(`ai_learning`) |
| 缓存 | Redis(Spring Cache + RedisTemplate + 热门题目 ZSet) |
| 大模型 | Ollama(本地部署 DeepSeek)+ 内置模拟生成器可切换 |
| 对象存储 | MinIO(AIStor)预签名上传/下载,可切本地磁盘 |
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios + ECharts |

## 前端(前后端分离)

```powershell
cd D:\dp\ai-learning\frontend
npm install --registry=https://registry.npmmirror.com   # 首次
npm run dev        # 开发:http://localhost:5173,/api 已代理到后端 8082
npm run build      # 构建:产出 dist/(可交给 Nginx 或后端静态目录)
```

页面结构(共 16 个页面):

| 端 | 页面 |
|---|---|
| 学生端 | 登录/注册、学习首页(概览 + AI 建议 + 薄弱点 + 热门题目)、智能刷题(4 种模式答题 + 即时判分)、错题本、考试中心、在线考试(倒计时 + 答题卡 + 自动暂存)、考试成绩(逐题解析 + AI 评语 + 排行)、学习报告(ECharts 趋势与知识点图表)、刷题记录、个人中心 |
| 管理端 | 题库管理(增删改查 + 多条件筛选)、AI 智能出题(生成 + 校验展示 + 预览/入库)、考试与组卷(AI 按题型配比组卷 + 发布/关闭 + 排行)、科目与知识点树、资源管理(上传/预签名/分类/删除)、AI 任务记录(提示词与模型返回留痕) |

## 目录结构

```
ai-learning
├── backend           Spring Boot 后端(见下方"快速开始")
│   └── src/main/java/com/ailearn
│       ├── common    统一响应、异常、分页、常量、用户上下文
│       ├── config    各类配置(MyBatis-Plus/Redis/OpenAPI/跨域/属性)
│       ├── security  JWT 与鉴权拦截器(@PublicApi / @AdminOnly)
│       ├── entity    实体(14 张表)
│       ├── mapper    MyBatis-Plus Mapper
│       ├── service   业务逻辑(含 AI 适配层与存储抽象)
│       ├── controller REST 接口
│       ├── ai        大模型适配层(Ollama / Mock)+ 提示词模板
│       └── storage   对象存储抽象(MinIO / 本地)
├── frontend          Vue3 前端
├── sql               建库建表与初始化数据
└── docs              设计文档
```

## 快速开始

### 日常启动(推荐:一键脚本)

```powershell
D:\dp\start-ai.cmd          # 检查 MySQL → 起 Redis(有 MinIO 则一并起)→ 起后端 → 起前端 → 打开页面
D:\dp\stop-ai.cmd           # 停止后端与前端(MySQL / Redis / MinIO 保持运行)
```

可选参数(直接调 ps1):`-NoFrontend`(只起后端)、`-NoOpen`(不自动开浏览器)、`-Rebuild`(先重新打包后端)、`-BackendPort 8083`(换端口)。

启动后:

| 入口 | 地址 | 账号 |
|---|---|---|
| 前端界面 | <http://localhost:5173> | `admin / 123456`(管理员)、`student / 123456`(学生) |
| 接口文档 | <http://localhost:8082/swagger-ui.html> | 无 |
| MinIO 控制台 | <http://localhost:9001> | minioadmin / minioadmin |

### 手动启动(等价步骤)

```powershell
# 1. 建库导入(仅首次;MySQL 3306,root/root)
Get-Content -Raw D:\dp\ai-learning\sql\ai_learning.sql | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --user=root --default-character-set=utf8mb4

# 2. Redis
D:\dp\tools\redis\redis-server.exe D:\dp\tools\redis\redis.windows.conf

# 3. MinIO(可选;未安装则 storage.type=local 用本地磁盘)
D:\dp\tools\minio\minio.exe server D:\dp\ai-learning\data\minio --console-address ":9001"

# 4. 后端(注意用 JDK 17)
$env:JAVA_HOME = 'D:\jdk17'
D:\dp\tools\maven\bin\mvn.cmd -s D:\dp\tools\maven-settings.xml -f D:\dp\ai-learning\backend\pom.xml -DskipTests clean package
D:\jdk17\bin\java.exe -jar D:\dp\ai-learning\backend\target\ai-learning-backend-1.0.0.jar

# 5. 前端
cd D:\dp\ai-learning\frontend
npm install --registry=https://registry.npmmirror.com   # 仅首次
npm run dev
```

> 用户初始密码在 SQL 里是明文,应用首次登录时自动升级为 BCrypt 密文。

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `AI_DB_HOST` / `AI_DB_PORT` / `AI_DB_NAME` | localhost / 3306 / ai_learning | 数据库 |
| `AI_DB_USER` / `AI_DB_PASSWORD` | root / root | 账号密码 |
| `AI_REDIS_HOST` / `AI_REDIS_PORT` | localhost / 6379 | Redis |
| `AI_PROVIDER` | mock | `mock` 内置模拟生成;`ollama` 调用本地大模型 |
| `OLLAMA_BASE_URL` / `OLLAMA_MODEL` | http://localhost:11434 / deepseek-r1:7b | 大模型地址与模型名 |
| `AI_STORAGE_TYPE` | minio | `minio` 或 `local` |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET` | http://localhost:9000 / minioadmin / minioadmin / ai-learning | 对象存储 |
| `AI_JWT_SECRET` / `AI_JWT_EXPIRE` | 内置默认值 / 1440 | 令牌密钥与有效期(分钟) |

## 大模型接入说明

`ai.provider` 决定出题与评分走哪条链路:

- **mock(默认)**:内置模拟生成器按知识点、难度、题型生成结构完整的题目,主观题按关键词覆盖率评分。
  不依赖任何外部服务,秒级返回,便于本地联调与演示。
- **ollama**:调用 `POST {OLLAMA_BASE_URL}/api/chat`,指定 `model` 后返回真实大模型结果。
  启动 Ollama 并拉取模型即可切换:`ollama pull deepseek-r1:7b`,然后设置 `AI_PROVIDER=ollama`。

两条链路使用同一套提示词模板(`resources/prompts/*.txt`)与同一套结果校验,切换不影响业务代码。

## 存储抽象与自动降级

`storage.type` 支持 `minio` 与 `local` 两种实现,业务只依赖 `FileStorage` 接口:

- **minio(项目默认)**:MinIO Java SDK 直连对象存储,启动时自动建桶,支持**预签名 PUT 直传**与**预签名 GET 下载**(前端直连对象存储,不占后端带宽)
- **local**:本地磁盘 + HMAC-SHA256 签名直链(带过期时间),用于离线开发
- **自动降级**:配置为 `minio` 但 **MinIO 连不上时,启动阶段自动降级为本地磁盘存储**并在日志中说明原因,
  保证系统在任何环境都能启动可用;启动 MinIO 后重启服务即自动切回。

实测:`storage.type=minio` 但 MinIO 未启动 → 实际生效 `local`;上传 / 签名直链下载(哈希一致)/ 伪造签名 403 / 删除后 404 / 预签名直传全部通过。

> 说明:MinIO 官方二进制在本机网络下下载受限(官方改名 AIStor,旧地址返回 410,新地址限速),
> 因此**真实 MinIO 实例上的端到端验证尚未完成**;已用本地存储模式完整验证了存储抽象层与全部接口。
> 你只需把 `minio.exe` 放到 `D:\dp\tools\minio\`(或用 Docker `minio/minio`),执行 `start-ai.cmd` 即会自动拉起并切回对象存储。

## 验证情况(本机实测)

| 测试脚本 | 覆盖内容 | 结果 |
|---|---|---|
| `D:\dp\tools\ai-learning-verify.ps1` 中的分步验证 | 认证/题库/缓存/刷题/考试/AI/存储 | 见下 |
| `D:\dp\tools\ai-e2e-test.ps1` | **端到端全链路**:注册登录 → AI 出题 → 刷题(含主观题 AI 评分)→ 错题本 → AI 组卷 → 考试 → 自动判分 → 学习报告 → AI 建议 → 权限校验 | **23 PASS / 0 FAIL** |
| `D:\dp\tools\frontend-api-check.ps1` | 前端调用的 37 个接口逐个打通校验 | **37 PASS / 0 FAIL** |
| `D:\dp\tools\miniprogram-check.ps1`(对比参考) | 小程序静态与接口校验 | 48 PASS / 0 FAIL |

关键实测数据(端到端一次运行):

```
AI 生成并入库        生成 2 道,入库 id=15;AI 提供方 mock/builtin-simulator
智能刷题             sessionId=8,4 题逐题判分,答对 4;故意答错 → addedToWrongBook=true
错题本               未掌握 1 题;错题重做模式抽到 1 题
AI 自动组卷          examId=4,5 题,总分 31
考试判分             客观分 21 + 主观分(AI)10 = 31;主观题 AI 评语已写入;生成 273 字学习建议
学习报告             作答 5 / 答对 4 / 正确率 80%;知识点分布 1 个;7 天趋势补零
AI 学习建议          268 字;可回查
缓存读写             6 个缓存接口「未命中 → 命中」两次均返回 200
```

MinIO 与本地存储均已实测(本地模式全链路:上传/下载/预签名直传/伪造签名 403/链接过期 403/删除后 404/分类识别/列表过滤)。

## 常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动报 `Unsupported class file major version` | 用了 JDK 21/22,必须用 JDK 17(`D:\jdk17`) |
| 前端 5173 打不开 | 先 `cd frontend && npm install`;或改用后端地址直接调接口文档 |
| 登录提示账号密码错误 | 初始账号 `admin/123456`、`student/123456`;密码首次登录后自动升级为 BCrypt |
| 上传资源报「MinIO 上传失败」 | MinIO 未启动且未走降级;正常情况会自动降级为本地磁盘,若仍报错请检查 `storage.type` 与日志 |
| 资源下载地址打不开 | 预签名/签名地址有有效期(默认 3600 秒),调 `/api/files/{id}/url` 刷新;本地模式的相对地址请拼上后端域名端口 |
| AI 出题/评分很慢或报错 | 默认 `mock` 是秒级模拟;若设了 `AI_PROVIDER=ollama` 请确认 Ollama 已启动且模型已拉取 |
| 缓存命中后接口 500 | 已修复:缓存用的 Jackson 必须关闭 `FAIL_ON_UNKNOWN_PROPERTIES`(VO 的计算属性无 setter),详见 `RedisConfig` |
| 组卷题数比要求少 | 题库题量不足;先用 AI 出题扩充题库 |
