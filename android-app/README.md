# 胚布成本计算器 Android APP

这是一个 Android WebView 壳项目，内置 `app/src/main/assets/www` 里的计算器页面。

## 构建

用 Android Studio 打开 `android-app` 文件夹，等待 Gradle 同步完成后：

1. 选择 `Build > Generate Signed Bundle / APK`
2. 选择 `APK`
3. 创建或选择签名证书
4. 生成 `release` 安装包

调试包可在终端运行：

```bash
gradle assembleDebug
```

生成后 APK 通常在：

```text
app/build/outputs/apk/debug/app-debug.apk
```
