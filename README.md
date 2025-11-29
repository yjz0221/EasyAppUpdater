# EasyAppUpdater



**EasyAppUpdater** 是一个轻量级、零依赖、高度可定制的 Android 应用内更新库。

它专为国内应用商店或企业内部应App用设计，无需 Google Play 服务，仅需几行代码即可实现版本检测、APK 下载、权限申请（Android 8.0+）及自动安装（Android 7.0+）。



## ✨ 核心特性



- **零依赖**：基于原生 `HttpURLConnection` 实现，不强制绑定 OkHttp 或 Retrofit，体积极小。
- **兼容性强**：完美适配 Android 5.0 (API 21) 至 Android 14+。
- **全自动适配**：
  - 自动处理 Android 7.0+ `FileProvider` 适配（防冲突设计）。
  - 自动处理 Android 8.0+ `REQUEST_INSTALL_PACKAGES` 权限申请与跳转。
- **高度灵活**：
  - 支持 GET / POST (JSON Body) / POST (Form 表单) 请求。
  - **UI 完全解耦**：内置默认弹窗，但支持通过策略模式完全自定义 UI（弹窗、进度条、错误提示）。
  - **解析自由**：不限制后端 JSON 格式，通过接口回调自行解析。



## 📦 引入依赖



1. 在根目录的 `build.gradle` 或 `settings.gradle` 中添加 JitPack 仓库：

Gradle

```
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

1. 在模块的 `build.gradle` 中添加依赖：

Gradle

```
dependencies {
    implementation 'com.github.YourUsername:EasyAppUpdater:1.0.0' // 请替换为实际发布的版本号
}
```



## 🚀 快速开始





### 1. 定义解析器 (Parser)

由于每个后端的 JSON 格式不同，你需要实现 `IUpdateParser` 接口来告诉库如何提取版本信息。

```kotlin
class MyUpdateParser : IUpdateParser {
    override fun parse(json: String): UpdateEntity {
        // 假设后端返回: {"code": 2, "msg": "修复Bug", "url": "http://...", "force": 0}
        val jsonObj = JSONObject(json)
        val serverVersion = jsonObj.optInt("code")
        val localVersion = BuildConfig.VERSION_CODE

        return UpdateEntity(
            hasUpdate = serverVersion > localVersion, // 是否有更新
            isForce = jsonObj.optInt("force") == 1,   // 是否强制更新
            versionName = "v2.0",                     // 新版本名
            content = jsonObj.optString("msg"),       // 更新日志
            downloadUrl = jsonObj.optString("url"),   // APK 下载地址
            extra = null                              // 可携带自定义数据传递给 UI
        )
    }
}
```



### 2. 一行代码检查更新



在 `MainActivity` 或设置页中调用：

```kotlin
EasyUpdater.Builder(this)
    .checkUrl("https://api.example.com/check_version") // 接口地址
    .jsonParser(MyUpdateParser())                      // 注入解析器
    .build()
    .check()
```



## 📖 进阶用法





### 1. 处理手动检查 (Toast 提示)

当用户点击“检查更新”按钮时，如果没有新版本，应当给予提示。

```kotlin
updater.check(
    isManual = true, // 开启手动模式
    onNoUpdate = { 
        Toast.makeText(this, "已经是最新版本了", Toast.LENGTH_SHORT).show()
    },
    onError = { e ->
        Toast.makeText(this, "检查失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
)
```



### 2. 使用 POST 请求



支持发送 JSON 或 表单参数。

```Kotlin
// 方式 A: POST JSON
EasyUpdater.Builder(this)
    .checkUrl("...")
    .postJson("{\"appId\": \"12345\"}") 
    .jsonParser(parser)
    .build()
    .check()

// 方式 B: POST 表单 (application/x-www-form-urlencoded)
val params = mapOf("channel" to "huawei", "version" to "100")
EasyUpdater.Builder(this)
    .checkUrl("...")
    .postForm(params)
    .jsonParser(parser)
    .build()
    .check()
```



### 3. 完全自定义 UI



如果你不喜欢默认的 `AlertDialog`，可以实现 `IUpdateUI` 接口，接管所有界面展示。

```kotlin
class CustomUI(private val context: Context) : IUpdateUI {
    
    // 1. 显示发现新版本弹窗
    override fun showUpdateDialog(info: UpdateEntity, onUpdate: () -> Unit, onCancel: () -> Unit) {
        MyPrettyDialog(context)
            .setTitle("发现新版本 ${info.versionName}")
            .setMessage(info.content)
            .setConfirmClickListener { onUpdate() } // 必须调用，通知库开始下载
            .setCancelClickListener { onCancel() }
            .show()
    }

    // 2. 显示权限申请弹窗 (Android 8.0+)
    override fun showPermissionDialog(onGoToSetting: () -> Unit, onCancel: () -> Unit) {
        // 提示用户需要安装权限
        MyPrettyDialog(context)
            .setMessage("安装应用需要授权，请前往设置开启。")
            .setConfirmClickListener { onGoToSetting() } // 库会自动跳转设置页
            .show()
    }

    // 3. 进度条与错误处理...
    override fun showDownloadProgress(progress: Int) { ... }
    override fun dismissDownloadProgress() { ... }
    override fun showError(e: Exception, msg: String) { ... }
}

// 使用自定义 UI
EasyUpdater.Builder(this)
    .checkUrl("...")
    .jsonParser(parser)
    .uiStrategy(CustomUI(this)) // 注入 UI 策略
    .build()
    .check()
```



### 4. 添加请求头 (Token)

```kotlin
EasyUpdater.Builder(this)
    .checkUrl("...")
    .headers(mapOf("Authorization" to "Bearer token..."))
    .jsonParser(parser)
    .build()
    .check()
```



## 🛠 混淆配置 (ProGuard)



本库核心类无需混淆，如果你开启了严格混淆，请添加以下规则：

```
-keep class com.github.yjz.easyupdater.** { *; }
-keep interface com.github.yjz.easyupdater.** { *; }
```





## 📄 License



```
Copyright 2025 [yjz0221]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```