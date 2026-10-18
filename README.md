# 图像对比

「图像对比」安卓 App，离线对比原图与 AI 生成图。

```bash
./gradlew testDebugUnitTest assembleRelease
```

发布包由 `.github/workflows/release.yml` 构建。仓库里配置了签名 secrets 时打正式签名，否则回退到 debug 签名。`v*` 标签会附带 APK 创建 GitHub Release。
