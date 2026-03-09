# Car Hosts Manager App

一个专为 Android 9 车机系统设计的 Hosts 管理应用，支持在代码中自定义 hosts 配置。

## 功能特性

- ✅ 支持 Android 9 (API 28) 车机系统
- ✅ 支持通过代码 programmatically 添加/删除 hosts 条目
- ✅ 图形界面手动管理 hosts
- ✅ Root 权限检测和系统 hosts 文件修改
- ✅ 配置文件备份和恢复
- ✅ 车机友好的深色主题 UI
- ✅ 支持 Leanback Launcher (车机启动器)

## 项目结构

```
CarHostsApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/carhosts/app/
│   │   │   ├── HostsManager.java    # 核心 hosts 管理类
│   │   │   └── MainActivity.java    # UI 界面
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 使用方法

### 1. 在代码中自定义 hosts

```java
// 获取 HostsManager 实例
HostsManager hostsManager = new HostsManager(context);

// 添加单个 hosts 条目
hostsManager.addHostEntry("127.0.0.1", "example.com");
hostsManager.addHostEntry("192.168.1.100", "api.mycar.com");

// 批量添加
Map<String, String> entries = new HashMap<>();
entries.put("ads.example.com", "127.0.0.1");
entries.put("tracking.example.com", "127.0.0.1");
hostsManager.addHostEntries(entries);

// 应用到系统 (需要 root)
if (hostsManager.hasRootAccess()) {
    boolean success = hostsManager.applyHosts();
}

// 保存配置到应用存储
hostsManager.saveToAppStorage();

// 从备份加载配置
hostsManager.loadFromAppStorage();
```

### 2. 使用 UI 界面

1. 输入 IP 地址和主机名
2. 点击 "Add Entry" 添加条目
3. 点击 "Apply to System" 应用到系统 (需要 root)
4. 长按列表中的条目可删除

## 编译要求

- Android Studio Arctic Fox 或更高版本
- JDK 8 或更高版本
- Android SDK API 28+

## 编译步骤

1. 用 Android Studio 打开 `CarHostsApp` 目录
2. 等待 Gradle 同步完成
3. 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)

或使用命令行:

```bash
cd CarHostsApp
./gradlew assembleDebug
```

## 注意事项

- **Root 权限**: 修改系统 hosts 文件需要设备已 root
- **Android 9**: 此应用针对 Android 9 (Pie) 优化，兼容 API 28
- **车机系统**: 支持车载信息娱乐系统，包含 Leanback Launcher 支持
- **备份**: 建议先保存配置备份，以便重新安装时恢复

## 许可证

MIT License
