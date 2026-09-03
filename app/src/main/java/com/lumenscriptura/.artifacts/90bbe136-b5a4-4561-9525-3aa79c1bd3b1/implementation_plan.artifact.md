# Fix Screen Orientation State-Reset and Search Lookups

This plan addresses several issues related to state preservation during screen rotation and improving search functionality for Bible books and references.

## Proposed Changes

### [Component] Android Manifest & App State

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/AndroidManifest.xml)
- Add `smallestScreenSize` to `android:configChanges` for `MainActivity` to prevent unnecessary restarts on some devices/scenarios.

#### [MODIFY] [App.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/java/com/lumenscriptura/App.kt)
- Ensure all key navigation state variables in `MainContent` use `rememberSaveable`.
- Re-check if any state is missing `rememberSaveable` (though most seem to have it already).
- Verify `showSearch` state.

### [Component] Bible Book Aliases

#### [MODIFY] [BibleBookAliases.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/java/com/lumenscriptura/BibleBookAliases.kt)
- Add missing aliases for Obadiah, Philemon, Jude, 2 John, and 3 John.
- Explicitly map "ob", "oba", "obad", "phm", "philem", "jd", "jude", "2john", "3john", etc.

### [Component] Search Functionality

#### [MODIFY] [SearchOverlay.kt](file:///C:/Users/waita/AndroidStudioProjects/bible-app/app/src/main/java/com/lumenscriptura/SearchOverlay.kt)
- Update `SmartSearchModal` to handle:
    - Book-only queries: If a book name is recognized, prepend a result for Chapter 1.
    - Single-chapter book references: Handle "Obadiah 15" as "Obadiah 1:15".
- Enhance search logic to prioritize reference matches.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device)

### Manual Verification
1.  **Rotation Test:**
    - Open the app to a specific book and chapter (e.g., Zechariah 3).
    - Rotate the device.
    - Confirm the app remains on Zechariah 3 and doesn't reset to Genesis 1.
2.  **Search Tests:**
    - Search for "Obadiah". Confirm "Obadiah 1" appears in results.
    - Search for "Psalms". Confirm "Psalms 1" appears in results.
    - Search for "Obadiah 15". Confirm it shows Obadiah 1:15.
    - Search for "2 John 5". Confirm it shows 2 John 1:5.
