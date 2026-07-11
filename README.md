# AR Traffic Sign

Android-приложение для распознавания дорожных знаков.

## Требования

- Android Studio или Android SDK с платформой Android 36.
- JDK 21; Gradle Wrapper может скачать нужный toolchain при первой сборке.
- `local.properties` с путем к Android SDK (`sdk.dir=...`), Android Studio создает его автоматически.
- Устройство или эмулятор с Android 9 (API 28) и выше. Для работы камеры нужно устройство с камерой.

## Сборка

Windows:

```powershell
.\gradlew.bat assembleDebug
```

Linux/macOS:

```bash
./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`.

## Установка

Подключить устройство с USB-отладкой и выполнить:

```powershell
.\gradlew.bat installDebug
```
