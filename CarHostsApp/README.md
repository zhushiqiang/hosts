# CarHostsApp - 车机系统 Hosts 管理器

一个专为 Android 9 车机系统设计的 Hosts 文件管理应用，支持自定义 hosts 配置。

## 特性

- ✅ **支持 Android 9 (API 28)** - 专为车机系统优化
- ✅ **车机友好界面** - 深色主题，大按钮设计，适合车载屏幕操作
- ✅ **自定义 Hosts** - 支持代码中和界面手动管理 hosts
- ✅ **Root 权限支持** - 可直接修改系统 hosts 文件
- ✅ **备份恢复** - 支持 hosts 配置文件备份和恢复
- ✅ **Leanback 支持** - 兼容车机 Leanback Launcher

## 快速开始

### 代码中使用

```java
// 初始化 HostsManager
HostsManager hostsManager = new HostsManager(context);

// 添加 hosts 条目
hostsManager.addHostEntry("127.0.0.1", "example.com");
hostsManager.addHostEntry("192.168.1.100", "api.mycar.com");

// 删除 hosts 条目
hostsManager.removeHostEntry("example.com");

// 应用 hosts 配置（需要 root 权限）
boolean success = hostsManager.applyHosts();

// 检查 root 权限
boolean hasRoot = hostsManager.hasRootAccess();
```

### 界面使用

1. 打开应用，授予 root 权限
2. 点击"+"按钮添加新的 hosts 条目
3. 输入 IP 地址和域名
4. 点击"应用 Hosts"按钮生效配置
5. 可导出/导入 hosts 配置备份

## 编译方式

### 使用 Android Studio
1. 用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 点击 Build -> Build Bundle(s) / APK(s) -> Build APK(s)

### 使用命令行
```bash
./gradlew assembleDebug
```

生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/carhosts/app/
├── MainActivity.java          # 主界面 Activity
├── HostsManager.java          # Hosts 管理核心类
├── HostsEntry.java            # Hosts 条目数据模型
└── HostsAdapter.java          # 列表适配器

app/src/main/res/
├── layout/                    # 布局文件
├── values/                    # 资源值（颜色、字符串等）
└── drawable/                  # 图形资源
```

## 权限说明

- **ROOT 权限** - 用于修改系统 hosts 文件 (`/system/etc/hosts`)
- **存储权限** - 用于导入/导出 hosts 配置文件

## 注意事项

⚠️ **警告**: 修改系统 hosts 文件需要 root 权限，错误配置可能导致网络连接问题。

⚠️ **车机系统**: 本应用专为车机系统设计，在普通手机上可能需要进行系统分区重新挂载操作。

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
