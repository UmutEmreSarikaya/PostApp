# PostApp

A small Android app built with Kotlin and Jetpack Compose. It fetches posts from JSONPlaceholder, caches them in Room, and supports browsing, editing, swipe-to-delete, and undo. Edits and deletions are local; refreshing replaces the saved posts with server data. Cached posts remain available offline after the initial fetch.

## Modular structure

The project separates the post feature into presentation, domain, and data layers, with shared database infrastructure. ViewModels expose UI state through StateFlow, and Hilt provides dependency injection.

| Module | Responsibility | Direct project dependencies |
| --- | --- | --- |
| `:app` | Application entry point, theme, navigation, and dependency wiring | `:post:presentation`, `:post:data` |
| `:post:presentation` | Compose list/detail screens, ViewModels, and UI models | `:post:domain` |
| `:post:domain` | `Post` model and `PostRepository` contract | None |
| `:post:data` | Repository implementation, API calls, caching, and model mapping | `:post:domain`, `:core:db` |
| `:core:db` | Room database, DAO, post entities, and sync state | None |

Presentation depends on the repository contract in domain; data implements that contract and coordinates network access and storage. Hilt connects the implementation at the application level. The domain module currently uses the Android library plugin, but has no dependencies on other project modules.

`build-logic` is a separate included Gradle build containing reusable convention plugins for Android libraries, Compose, Hilt/KSP, and Kotlin/JVM libraries. Shared dependency versions live in `gradle/libs.versions.toml`.

## Main dependencies

- **UI:** Jetpack Compose, Material 3, Navigation Compose, and Coil for images.
- **State and async work:** AndroidX Lifecycle, Kotlin coroutines, and Flow.
- **Networking:** Retrofit, Gson, and OkHttp logging.
- **Persistence:** Room, with KSP for code generation.
- **Dependency injection:** Hilt, with KSP.
- **Testing:** JUnit, Robolectric, and coroutine test dispatchers; repository tests cover caching, refresh, local changes, and error propagation. ViewModel tests cover loading, refresh confirmation, deletion/undo, edit validation, navigation, and saving.

## Note

Modularization is not necessary for a project this small. It is used here deliberately to demonstrate skills in modular architecture, separation of concerns, dependency management, and reusable build configuration.
