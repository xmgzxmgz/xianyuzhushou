# 闲鱼助手

Android 自动化工具，通过 OCR 识别和无障碍服务实现闲鱼 App 的智能辅助操作。

## 功能

- 📸 **截图识别** — 自动截取闲鱼页面内容
- 🔍 **OCR 文字识别** — 支持 Tesseract 和 ML Kit 双引擎
- 🧠 **语义解析** — 智能理解页面结构与商品信息
- ♿ **无障碍自动化** — 通过 AccessibilityService 自动执行操作
- 🖼️ **图像预处理** — 自适应二值化、降噪、增强识别精度

## 技术栈

- **语言**: Kotlin
- **构建**: Gradle
- **OCR**: Tesseract + Google ML Kit
- **自动化**: Android AccessibilityService

## 构建

```bash
./gradlew assembleDebug
```

生成 APK 位于 `app/build/outputs/apk/debug/`

## 权限要求

- 无障碍服务权限（用于自动化操作）
- 屏幕截图权限（用于 OCR 识别）
- 存储权限（用于保存识别结果）

## License

MIT
