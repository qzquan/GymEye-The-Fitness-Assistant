
## 项目简介

GymEye 是一款基于 AI 图像识别技术的健身器械助手。用户使用手机摄像头扫描健身房中的器械，系统通过 YOLOv8 模型在本地实时识别器械类型，并展示名称、训练部位、使用步骤、注意事项及标准教学视频。

面向健身新手和不熟悉器械的人群，帮助快速了解器械用途，避免误用，降低入门门槛。

> **GymEye —— 一扫即懂的健身助手**

---

## 核心功能

### 1. AI 实时扫描识别
- CameraX 摄像头实时取景，YOLOv8 模型本地推理，无需网络
- TFLite Float32 精度，输入 640×640，4 线程推理，置信度阈值 0.5
- 背压策略 `STRATEGY_KEEP_ONLY_LATEST` 保证流畅度

### 2. 器械知识库
- 器械列表浏览、中文搜索、按训练部位筛选
- 详情页展示名称、训练部位、难度等级、使用步骤、注意事项、常见错误
- 肌肉解剖图本地映射，推荐动作卡片关联

### 3. 教学视频播放
- ExoPlayer 集成，三层视频源降级策略（本地 raw → 后端 URL → 占位图）
- 自定义控制条：百分比映射 SeekBar，4 秒自动隐藏，异常加载提示

### 4. 训练记录与数据统计
- WorkoutLogBottomSheet 打卡：快捷 Chip 选择组数/次数/重量/时长，训练感受三选一
- StatisticsActivity 统计页：摘要卡片（周次数/总时长/最常部位/连续天数）、MPAndroidChart 贝塞尔折线图、90 天训练日历热力图（自定义 GridLayout 四级颜色编码）

### 5. 用户系统
- JWT 认证，`requireAuth` / `optionalAuth` 双中间件
- 注册/登录/自动登录/游客模式，LoginActivity 内嵌马赛克背景漂移动画
- 用户数据隔离校验，SharedPreferences 本地持久化

---

## 实际技术栈

### Android 前端
| 模块 | 选型 |
|------|------|
| 摄像头 | CameraX (`ImageAnalysis.RGBA_8888`) |
| AI 推理 | TensorFlow Lite (Java API，原生 MappedByteBuffer 加载) |
| 视频播放 | VideoView (ExoPlayer)，自定义控制条 |
| 图表 | MPAndroidChart (LineChart + CUBIC_BEZIER) |
| 网络 | HttpURLConnection 自封装 (`GymEyeApiClient.java`) |
| 自定义 UI | MosaicBackgroundView（274 行 Canvas 绘制 + ValueAnimator 漂移动画）、GridLayout 日历热力图 |

### AI 模型
- YOLOv8 目标检测 → TensorFlow Lite 转换 → APK assets 内置
- Roboflow 数据标注，Float32 精度，640×640 RGB 输入，[1,7,8400] 输出

### 后端 (Node.js / Express)
- 8 个路由模块（user / equipment / history / workouts / exercise-videos / bodyParts / difficultyLevels）
- JSON 文件存储（`backend/data/db.json`），原子写入（tmp + rename）+ mutationQueue 序列化锁 + 自动初始化 + 自动迁移
- JWT 认证双中间件，统一 AppError 错误处理，camelCase / snake_case 参数双兼容

---

## 系统架构

```
Android 前端 (Java)
├── CameraX (实时取景)
├── TFLite (YOLOv8 本地推理)
├── ExoPlayer (视频播放)
├── MPAndroidChart (数据可视化)
└── HttpURLConnection (GymEyeApiClient)
        │
        │ RESTful JSON / Bearer JWT
        ▼
Node.js / Express 后端
├── 8 个路由模块
├── JWT 认证中间件
├── 统一错误处理
└── JSON 文件存储 (原子写入 + 序列化锁)
```

---

## 用户使用流程

1. 打开 App → 自动登录 / 选择账号 / 游客模式
2. 主页面：扫描入口 | 器械知识 | 训练记录 | 退出登录
3. 扫描器械 → 实时识别 → 点击查看详情
4. 详情页：视频播放 + 肌肉解剖图 + 推荐动作卡片
5. 查看动作步骤 → 播放教学视频 → 记录训练
6. 统计页：查看摘要卡片、折线图、热力图、最近记录

---

## 首批支持器械

坐姿推胸机、坐姿腿弯举、坐姿腿屈伸、史密斯机、高位下拉机、坐姿划船机、腿举机、蝴蝶夹胸机

---

## 10 周开发回顾

| 阶段 | 周期 | 核心产出 |
|------|------|---------|
| 迭代 0：基础设施 | 第 1-2 周 | labels.txt、项目骨架搭建、数据采集与 Roboflow 标注 |
| 迭代 1：Alpha 核心闭环 | 第 3-5 周 | YOLOv8 V1.0 → TFLite，扫描→识别→详情→视频完整链路 |
| 迭代 2：Beta 功能扩展 | 第 6-8 周 | 训练记录与统计可视化、UI 优化、模型准确率提升、视频功能修复 |
| 迭代 3：Final 交付收尾 | 第 9-10 周 | 模型 V3.0、Bug 修复、系统测试 90 用例、答辩材料 |

---

## 团队分工

| 角色 | 职责 | 涉及技术 |
|------|------|---------|
| PM / UI | 需求分析、原型设计、用户测试、答辩材料 | Figma |
| AI 工程师 | 数据采集、Roboflow 标注、YOLOv8 训练、TFLite 转换 | Python / YOLOv8 / Roboflow / TFLite |
| Android 工程师 | CameraX 集成、YOLOv8Detector 封装、6 个 Activity、自定义 View、视频播放 | Java / Android SDK / CameraX / ExoPlayer |
| Backend 工程师 | 8 个路由模块、JWT 认证、JSON 存储层、Gradle 构建集成、训练统计 | Node.js / Express / JWT |

---

## 质量保障

- **系统测试**：依据 GB/T 8567-1988 标准，7 个模块 90 用例，通过率 97.8%（88/90）
- **缺陷管理**：发现 12 个缺陷（0 致命 / 4 严重 / 8 一般），全部修复
- **异常处理**：覆盖 6 种异常场景（权限拒绝 / 模型加载失败 / 识别失败 / 网络异常 / 视频加载失败 / Token 过期）
- **数据安全**：原子写入 + 序列化锁 + 用户隔离校验

---

## 监测与报告机制

- **周会驱动**：每周日晚 40 分钟敏捷站会，三段式汇报（上周产出 → 下周计划 → 当前阻塞）
- **Git 分支管理流**：GitFlow 工作流，严禁直接 Push main，PR + 交叉测试确认后 Merge
- **任务看板追踪**：To Do → In Progress → Testing → Done 四阶段流转，卡片化任务，状态透明公开

---

## 后续规划

| 阶段 | 时间 | 目标 |
|------|------|------|
| Phase I 工程优化 | 1-2 月 | 模型扩充至 8 类、INT8 量化、Room 本地缓存、CI/CD |
| Phase II 功能增强 | 3-6 月 | MediaPipe 动作姿态识别、个性化推荐、云端部署、HTTPS |
| Phase III 生态扩展 | 6-12 月 | iOS 版本、微信小程序、社区功能、多语言 |

---

## 快速开始

```bash
# 一键启动（启动后端 + 编译 APK + 安装到设备）
./run-dev.cmd

# 或分步启动
cd backend && npm install && npm start      # 后端 → localhost:8080
cd android_project && ./gradlew installDebug # Android → 安装到设备
```

- Node.js 未在 PATH 中时，设置环境变量 `GYMEYE_NODE=C:\path\to\node.exe`
- 后端首次启动自动从 `labels.txt` 初始化器械数据，无需手动配置数据库
- API 冒烟测试：`npm run smoke`
