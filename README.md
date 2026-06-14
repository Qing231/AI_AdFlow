# AIAdFlow - AI广告信息流 Demo

## 项目简介

AIAdFlow 是一个基于 **Kotlin + Jetpack Compose + MVVM** 构建的 Android 广告信息流应用 Demo。

项目模拟真实移动端广告流场景，支持频道切换、自然语言搜索、多样式广告卡片、广告详情页、点赞收藏分享、下拉刷新、上拉加载、AI 摘要生成、AI 智能标签生成等功能。

项目以「AI + 信息流」为主题，重点展示：

* Jetpack Compose 声明式 UI 开发
* MVVM 架构实践
* Android 状态管理
* 页面状态同步
* 信息流场景设计
* AI 内容生成能力
* 视频广告展示能力

---

## 功能特性

### 首页信息流

* 单列广告信息流布局
* LazyColumn 流畅滚动
* 列表位置保持
* 空状态展示

### 多样式广告卡片

支持：

* SmallImage（小图广告）
* LargeImage（大图广告）
* ImageText（图文广告）
* Video（视频广告）

### 频道切换

支持：

* 全部
* 推荐
* 电商
* 本地

### 搜索功能

支持：

* 品牌名搜索
* 标题搜索
* AI 摘要搜索
* 标签搜索

支持自然语言搜索：

```text
适合学生党的 AI 工具
适合健身用户的视频广告
附近优惠咖啡推荐
```

### 标签过滤

支持：

* 标签点击筛选
* 多条件组合过滤
* 当前筛选状态展示
* 一键清除筛选

### 广告详情页

支持：

* 图文详情页
* 视频详情页
* 返回首页
* 状态同步

### 点赞收藏分享

支持：

* 点赞
* 收藏
* 分享
* 本地状态持久化

### 下拉刷新

支持：

* PullToRefresh
* 当前筛选条件重新加载

### 上拉加载更多

支持：

* 自动分页加载
* 加载中状态
* 无更多数据状态
* 加载失败重试

### 视频能力

支持：

* 视频播放
* 暂停
* 静音
* 视频详情页播放器

基于：

* AndroidX Media3
* ExoPlayer

### AI能力

#### AI摘要

自动生成广告摘要：

```text
AI摘要：突出广告亮点、适用人群和推荐原因
```

#### AI智能标签

自动生成：

```text
#学生党
#效率工具
#运动健身
```

#### 对话式搜索

支持自然语言理解：

```text
适合学生党的 AI 工具
```

返回：

* AI理解结果
* 推荐标签
* 广告排序结果

---

## 技术架构

```text
MockAdProvider / OpenAI API
          ↓
      Repository
          ↓
      ViewModel
          ↓
       UiState
          ↓
      Compose UI
```

架构模式：

```text
MVVM
```

---

## 技术栈

| 技术                 | 说明        |
| ------------------ | --------- |
| Kotlin             | 开发语言      |
| Jetpack Compose    | UI框架      |
| Material3          | UI组件      |
| ViewModel          | 状态管理      |
| StateFlow          | 响应式状态流    |
| Navigation Compose | 页面导航      |
| Media3             | 视频播放      |
| ExoPlayer          | 视频播放器     |
| SharedPreferences  | 本地状态存储    |

---

## 项目结构

```text
 C:\AndroidProjects
  ├─ settings.gradle.kts
  ├─ build.gradle.kts
  ├─ gradle.properties
  ├─ gradlew / gradlew.bat
  ├─ gradle/
  │  ├─ libs.versions.toml
  │  └─ wrapper/
  ├─ docs/
  ├─ app/
  │  ├─ build.gradle.kts
  │  ├─ proguard-rules.pro
  │  └─ src/
  │     ├─ main/
  │     │  ├─ AndroidManifest.xml
  │     │  ├─ java/com/example/aiadflow/
  │     │  │  ├─ MainActivity.kt
  │     │  │  ├─ data/
  │     │  │  │  ├─ local/        本地点赞/收藏状态持久化
  │     │  │  │  ├─ mock/         Mock 广告数据
  │     │  │  │  ├─ model/        AdItem、Channel、AdType 等模型
  │     │  │  │  ├─ repository/   广告数据仓库
  │     │  │  │  ├─ search/       智能搜索
  │     │  │  │  ├─ summary/      AI 摘要
  │     │  │  │  └─ tag/          智能标签
  │     │  │  └─ ui/
  │     │  │     ├─ card/         广告卡片
  │     │  │     ├─ channel/      频道
  │     │  │     ├─ common/       通用 UI/样式辅助
  │     │  │     ├─ detail/       详情页
  │     │  │     ├─ feed/         首页 ViewModel
  │     │  │     ├─ home/         首页
  │     │  │     ├─ interaction/  点赞、分享、收藏交互
  │     │  │     ├─ load/         拖动加载/加载状态
  │     │  │     ├─ media/        图片/视频播放器、缓存
  │     │  │     ├─ search/       搜索栏
  │     │  │     ├─ summary/      AI 摘要 UI
  │     │  │     ├─ tag/          标签 UI
  │     │  │     ├─ theme/        主题、颜色、尺寸
  │     │  │     └─ video/        视频入口封装
  │     │  └─ res/
  │     │     ├─ raw/             adv1.mp4 ~ adv5.mp4
  │     │     ├─ drawable/
  │     │     ├─ mipmap-*/
  │     │     ├─ values/
  │     │     └─ xml/
  │     ├─ test/
  │     └─ androidTest/

```

---

# 模块划分

## 首页信息流模块

负责：

* 频道切换
* 搜索
* 标签过滤
* 广告列表展示
* 下拉刷新
* 上拉加载

核心组件：

```text
HomeScreen
ChannelTabs
SearchBar
AdCard
```

---

## 广告卡片模块

支持：

```text
SmallImage
LargeImage
ImageText
Video
```

核心组件：

```text
AdCard
SmallImageAdContent
LargeImageAdContent
ImageTextAdContent
VideoAdContent
```

---

## 广告详情模块

负责：

* 图文详情
* 视频详情
* 返回首页

核心组件：

```text
AdDetailScreen
ImageTextDetailContent
VideoDetailContent
```

---

## AI模块

### AI摘要

负责：

* 广告内容摘要生成
* 摘要缓存
* 摘要展示

### AI标签

负责：

* 智能标签生成
* 标签缓存
* 标签展示

---

## 对话式搜索模块

负责：

* 自然语言理解
* 意图扩展
* 广告排序
* 推荐标签生成

---

## 视频模块

负责：

* 视频播放
* 暂停
* 静音
* 详情页播放器

---

## 本地状态模块

负责：

* 点赞状态管理
* 收藏状态管理
* 状态持久化

存储方式：

```text
SharedPreferences
```

---

# 如何运行

## 环境要求

### Android Studio

推荐版本：

```text
Android Studio Narwhal / Panda 及以上
```

### JDK

```text
JDK 17+
```

### Android SDK

```text
API 35+
```

### Gradle

项目已内置 Wrapper：

```bash
./gradlew
```

无需额外安装。

---

## 克隆项目

```bash
git clone https://github.com/your-name/AIAdFlow.git
```

进入项目：

```bash
cd AIAdFlow
```

---

## 配置 OpenAI API（可选）

在：

```properties
local.properties
```

增加：

```properties
OPENAI_API_KEY=sk-xxxxxxxx
```

未配置时：

* AI摘要使用 Mock 模式
* AI标签使用 Mock 模式

项目仍可正常运行。

---

## 运行项目

### 真机运行

开启：

```text
开发者模式
USB调试
```

连接设备：

```text
魅族21
Android 16
```

点击：

```text
Run > app
```

即可运行。

---

# 开发规范

## 代码规范

遵循：

```text
Kotlin Coding Convention
```

### 命名规范

类：

```kotlin
AdFeedViewModel
```

Composable：

```kotlin
HomeScreen()
```

状态：

```kotlin
uiState
```

事件：

```kotlin
onLikeClick()
```

---

## Compose规范

统一使用：

```kotlin
@Composable
```

禁止：

```text
XML + findViewById 混用
```

---

## 状态管理规范

统一：

```text
UI
↓
ViewModel
↓
Repository
↓
DataSource
```

禁止：

```text
Composable直接修改Repository
```

---

## Git规范

提交格式：

```text
feat：首页频道切换

fix：修复搜索过滤问题

refactor：重构广告卡片

docs：补充README
```

---

# 测试

项目测试覆盖：

## 功能测试

* 首页展示
* 搜索功能
* 标签过滤
* 频道切换
* 点赞收藏
* 分享功能
* 广告详情页

## 兼容性测试

* 真机测试
* 字体缩放测试

## 性能测试

* 信息流滚动性能
* 视频播放性能
* 状态同步性能

## 异常测试

* 空数据测试
* 图片加载失败
* 视频地址为空

---

# AI声明

本项目部分代码、文档以及设计过程使用 AI 工具辅助完成。

使用工具包括：

* ChatGPT
* Codex
* Android Studio AI Assistant

AI主要用于：

* UI设计建议
* Compose页面生成
* 架构设计讨论
* README编写
* 测试用例整理
* AI摘要生成
* AI标签生成

所有代码均经过：

* 人工审核
* 人工修改
* 人工测试
* 人工验收

最终实现结果由开发者负责。

---

# 后续规划

未来计划扩展：

* Room数据库
* DataStore状态存储
* 网络广告接口
* 图片缓存优化
* 视频自动播放
* AI Agent推荐系统
* 用户画像分析
* 服务端埋点统计
