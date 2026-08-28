# Implementation Plan - Port LumenScriptura to Compose Multiplatform

Port the C# .NET 10 "LumenScriptura" Bible App to a Compose Multiplatform (CMP) application in Kotlin, targeting Android.

## Proposed Changes

### [Scaffolding] CMP Project Structure

Setup the project with a `composeApp` module containing `commonMain` (shared logic and UI) and `androidMain` (Android specific entry point).

#### [NEW] [settings.gradle.kts](file:///C:/Users/waita/AndroidStudioProjects/bible-app/settings.gradle.kts)
#### [NEW] [gradle.properties](file:///C:/Users/waita/AndroidStudioProjects/bible-app/gradle.properties)
#### [NEW] [libs.versions.toml](file:///C:/Users/waita/AndroidStudioProjects/bible-app/gradle/libs.versions.toml)
#### [NEW] [build.gradle.kts](file:///C:/Users/waita/AndroidStudioProjects/bible-app/build.gradle.kts) (Root)
#### [NEW] [build.gradle.kts](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/build.gradle.kts) (Module)

### [Data Models] Kotlin Data Classes

Port C# models to Kotlin `@Serializable` data classes.

#### [NEW] [Book.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/Book.kt)
#### [NEW] [Verse.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/Verse.kt)
#### [NEW] [Note.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/Note.kt)
#### [NEW] [Bookmark.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/Bookmark.kt)
#### [NEW] [Highlight.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/Highlight.kt)
#### [NEW] [HistoryItem.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/models/HistoryItem.kt)

### [Services] Logic Porting

Port `NoteParserService.cs` and implement `BibleService` to load and query `kjv.json`.

#### [NEW] [NoteParserService.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/services/NoteParserService.kt)
#### [NEW] [BibleService.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/services/BibleService.kt)

### [UI] Compose Components

Build the shared UI for reading, notes, and search.

#### [NEW] [App.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/commonMain/kotlin/com/lumenscriptura/App.kt)
#### [NEW] [MainActivity.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/composeApp/src/androidMain/kotlin/com/lumenscriptura/MainActivity.kt)

## Verification Plan

### Automated Tests
- Port existing logic tests or create new ones for `NoteParserService`.
- Verify JSON parsing of `kjv.json`.

### Manual Verification
- Run the `androidApp` on an emulator/device.
- Verify Bible text rendering and scripture reference parsing in notes.
