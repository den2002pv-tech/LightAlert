# LightAlert

LightAlert — нативное Android-приложение для отображения расписаний плановых и аварийных отключений электроэнергии.

На текущем этапе создана базовая Android-основа. Интеграция с Telegram, получение расписаний, локальное хранилище, уведомления и виджеты пока не реализованы.

## Технологии

- Kotlin
- Jetpack Compose и Material 3
- MVVM
- Gradle Kotlin DSL
- Gradle Version Catalog
- AndroidX Lifecycle, Coroutines и Flow
- Минимальная версия Android: 8.0 (API 26)

## Структура проекта

```text
LightAlert/
├── .github/workflows/android-build.yml
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/app/lightalert/
│       │   │   ├── core/
│       │   │   ├── data/
│       │   │   ├── domain/model/
│       │   │   └── presentation/
│       │   │       ├── home/
│       │   │       └── theme/
│       │   └── res/
│       └── test/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

### Архитектурные слои

- `presentation` — Compose-интерфейс и ViewModel.
- `domain` — модели и бизнес-правила.
- `data` — будущие репозитории и источники данных.
- `core` — будущие общие компоненты приложения.

## Локальная сборка

Требуется JDK 17 или совместимая более новая версия.

Сборка debug APK:

```bash
./gradlew assembleDebug
```

Запуск unit-тестов:

```bash
./gradlew testDebugUnitTest
```

Готовый APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Android Studio для сборки не требуется. Gradle Wrapper самостоятельно загружает нужную версию Gradle.

## Сборка через GitHub Actions

Workflow находится в `.github/workflows/android-build.yml`.

Он запускается:
- при push в `main`;
- при pull request в `main`;
- вручную через вкладку Actions.

Workflow устанавливает JDK и Android SDK, запускает unit-тесты, собирает debug APK и публикует его как artifact с именем `LightAlert-debug-apk`.

Чтобы скачать APK:
1. Откройте вкладку **Actions** репозитория.
2. Выберите успешный запуск **Android CI**.
3. В разделе **Artifacts** скачайте `LightAlert-debug-apk`.

## Текущий статус

- [x] Базовый Android-проект
- [x] Главный экран на Jetpack Compose
- [x] Светлая и тёмная темы
- [x] Начальная MVVM-структура
- [x] Domain-модель расписания
- [x] Gradle Wrapper
- [x] Unit-тесты domain-модели
- [x] GitHub Actions для тестирования и сборки APK
- [ ] Подключение TDLib и авторизация Telegram
- [ ] Выбор Telegram-каналов и групп
- [ ] Получение и хранение сообщений
- [ ] Распознавание расписаний
- [ ] Android-виджет
- [ ] Уведомления об отключениях

Все будущие функции будут добавляться отдельными этапами. На данный момент приложение не подключается к Telegram и не получает реальные данные об отключениях.
