# 歌笺（RecordMyLive）

歌笺是一款 Android 演出记录应用，用来整理看过和计划参加的演出、相关花费与喜欢的艺人。项目目前处于 `0.0-alpha` 阶段。

## 主要功能

- **演出记录**：按日历查看、添加、编辑和删除演出，记录日期、地点、艺人、曲目等信息；详情页可管理费用，并添加日历提醒。
- **时间线与统计**：按时间浏览演出；按月、年或全部时间查看场次、城市、时长、花费及排行。
- **个人资料**：设置昵称和头像，维护喜欢的艺人列表及顺序。
- **附近同好**：通过 Google Nearby Connections 发现附近用户，查看共同喜欢的艺人；可选择开启匹配通知。此功能需要设备支持 Google Play services，并按系统提示授予蓝牙、附近设备等权限。
- **崩溃报告**：发生未捕获异常时显示报告，可复制、导出为文本或通过邮件应用发送。

## 开发与运行

项目是单模块 Java Android 应用，使用 Gradle Wrapper、Android Gradle Plugin 9.4.1、Room、AndroidX Navigation、Material Components 和 Google Play services Nearby。

1. 使用兼容 AGP 9.4.1 的 Android Studio 打开项目根目录，安装项目要求的 Android SDK（`compileSdk` 为 36，minor API level 为 1）并完成 Gradle 同步。
2. 连接 Android 12（API 31）或更新版本的设备或模拟器，运行 `app` 配置。附近同好功能建议在两台具备 Google Play services 的真机上测试。
3. 也可以在项目根目录执行 `./gradlew assembleDebug`；Windows PowerShell 使用 `./gradlew.bat assembleDebug`。生成的 APK 位于 `app/build/outputs/apk/debug/`。

运行单元测试：`./gradlew testDebugUnitTest`。运行设备测试：`./gradlew connectedDebugAndroidTest`（需要已连接的设备或模拟器）。

## 配置

崩溃报告邮件的收件地址由根目录 `gradle.properties` 中的 `crashReportEmail` 设置，在构建时生成 `crash_email_address` 字符串资源。修改地址后需要重新构建应用。该地址会包含在 APK 中，不应作为秘密信息存放。

## 代码结构

| 路径 | 内容 |
| --- | --- |
| `app/src/main/java/rachel/materialapps/recordmylive/ui/` | Activity、Fragment、页面组件与 ViewModel |
| `app/src/main/java/rachel/materialapps/recordmylive/logic/` | Room 数据层、仓库、附近发现与业务逻辑 |
| `app/src/main/java/rachel/materialapps/recordmylive/util/` | 崩溃报告的采集、格式化和本地存储 |
| `app/src/main/res/navigation/` | 页面导航图 |
| `app/src/test/`、`app/src/androidTest/` | 单元测试与设备测试 |

演出、费用和喜欢的艺人等数据存储在应用本地数据库中；卸载应用可能移除本地数据。
