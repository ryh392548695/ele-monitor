# EleMonitor - 饿了么盯商圈监控

[![Build APK](https://github.com/YOUR_USERNAME/ele-monitor/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/ele-monitor/actions/workflows/build.yml)

## 功能

- **后台运行**：关闭 App、锁屏、最小化都能持续监控
- **声音提醒**：数据异常时播放系统警报声
- **震动提醒**：配合声音双重提醒
- **系统通知**：下拉通知栏显示异常详情
- **定时查询**：可自定义查询间隔（0.5-60分钟）
- **条件自定义**：监控列名、比较方式、阈值均可自定义
- **防重复提醒**：冷却时间设置，避免频繁打扰
- **开机自启**：手机重启后自动恢复监控

## 快速开始

### 1. 下载 APK

点击右侧 **Releases** → 下载最新版本的 `app-debug.apk`

或者点击上方 **Actions** → 选择最新 workflow → 下载 Artifacts 中的 `app-debug`

### 2. 安装

1. 将 APK 文件传输到 Android 手机
2. 点击安装（可能需要允许"未知来源"安装）
3. 打开 App

### 3. 配置后台运行（重要！）

**必须设置，否则锁屏后会被系统杀死：**

| 品牌 | 设置路径 |
|------|---------|
| 华为 | 设置 → 电池 → 应用启动管理 → 盯商圈监控 → 手动管理 → 允许后台运行 |
| 小米 | 设置 → 省电与电池 → 应用智能省电 → 盯商圈监控 → 无限制 |
| OPPO | 设置 → 电池 → 应用耗电管理 → 盯商圈监控 → 允许后台运行 |
| vivo | 设置 → 电池 → 后台耗电管理 → 盯商圈监控 → 允许后台高耗电 |
| 三星 | 设置 → 电池 → 后台使用限制 → 未监视的应用 → 添加 |
| 一加 | 设置 → 电池 → 电池优化 → 盯商圈监控 → 不优化 |

### 4. 使用

1. 打开 App，会自动加载饿了么盯商圈页面
2. 设置查询间隔、监控列名、阈值等参数
3. 点击"启动监控"
4. 可以按 Home 键返回桌面，锁屏等待
5. 有异常时会：**声音 + 震动 + 通知**

## 自行编译

### 使用 Android Studio

1. 克隆本仓库
   ```bash
   git clone https://github.com/YOUR_USERNAME/ele-monitor.git
   ```

2. 用 Android Studio 打开项目

3. 点击 **Build → Build Bundle(s) / APK(s) → Build APK(s)**

4. APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`

### 使用命令行

```bash
# 确保已安装 Android SDK
export ANDROID_HOME=/path/to/android-sdk

# 编译
./gradlew assembleDebug

# 输出
# app/build/outputs/apk/debug/app-debug.apk
```

## 技术架构

- **Frontend**: Android Native (Java)
- **Web Engine**: WebView (加载饿了么页面)
- **Background**: Foreground Service + WakeLock
- **Notification**: NotificationManager + NotificationChannel
- **Audio**: MediaPlayer + RingtoneManager
- **Scheduling**: Handler + Runnable

## 文件结构

```
app/
├── src/main/
│   ├── AndroidManifest.xml          # 应用配置
│   ├── java/com/elemonitor/
│   │   ├── MainActivity.java        # 主界面
│   │   ├── MonitorService.java      # 后台服务（核心）
│   │   ├── MonitorConfig.java       # 配置类
│   │   └── BootReceiver.java        # 开机自启
│   └── res/
│       ├── layout/activity_main.xml # 主界面布局
│       └── values/                  # 字符串、颜色、主题
└── build.gradle                     # 构建配置
```

## 常见问题

**Q: 锁屏后没有提醒？**
A: 检查电池优化设置，确保允许后台运行。

**Q: 声音太小？**
A: 调大手机媒体音量，或更换系统铃声为更响亮的。

**Q: 页面加载失败？**
A: 检查网络连接，饿了么页面需要登录。

**Q: 如何停止监控？**
A: 打开 App 点击"停止监控"，或从最近任务中划掉 App。

**Q: 如何更新？**
A: 下载最新 APK 覆盖安装即可，设置会保留。

## 自动更新

本仓库使用 GitHub Actions 自动编译 APK：

- 每次推送到 `main` 分支会自动触发构建
- 构建成功后自动发布到 Releases
- 可以在 Actions 页面下载最新 APK

## 免责声明

本工具仅用于个人学习和数据监控，请遵守饿了么平台使用协议。

## License

MIT License
