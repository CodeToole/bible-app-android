# Rename App branding to "Bible Study App"

This plan covers renaming the app from "LumenScriptura" to "Bible Study App" across Android resources, Manifest, Compose UI, and build configuration.

## Proposed Changes

### [Component Name]

#### [NEW] [strings.xml](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/res/values/strings.xml)
- Create a new `strings.xml` file with the `app_name` resource.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/AndroidManifest.xml)
- Update `android:label` to use `@string/app_name`.

#### [MODIFY] [App.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/java/com/lumenscriptura/App.kt)
- Ensure the drawer header matches the requested branding. (It already appears to be "BIBLE STUDY APP", but I will verify and update if needed).

#### [MODIFY] [build.gradle.kts](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/build.gradle.kts)
- Update `applicationId` to `com.waitaminutedigital.biblestudy`.

#### [MODIFY] [README.md](file:///C:/Users/waita/AndroidStudioProjects/bible-app/README.md)
- Update the title and any references to "LumenScriptura" to "Bible Study App".

## Verification Plan

### Automated Tests
- None specified, but I will ensure the project compiles.

### Manual Verification
- The user can verify the app title in the launcher and the UI headers.
- **IMPORTANT**: If `applicationId` is changed, Firebase services (if any) might stop working until a new `google-services.json` is provided with the matching package name.
