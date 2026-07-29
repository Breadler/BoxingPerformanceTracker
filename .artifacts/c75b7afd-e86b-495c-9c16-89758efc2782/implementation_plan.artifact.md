# Fix Gradle Plugin Resolution Error

The build is failing because Gradle cannot find the Android Gradle Plugin (`com.android.application`). This is due to missing repository configurations in the `settings.gradle.kts` file.

## User Review Required

> [!IMPORTANT]
> I will be updating the `settings.gradle.kts` file to include standard repository configurations (`google()`, `mavenCentral()`). This is a standard requirement for Android projects using modern Gradle versions.

## Proposed Changes

### Android Project Configuration

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/User/Documents/GitHub/BoxingPerformanceTracker/android/settings.gradle.kts)
- Add `pluginManagement` block to define where Gradle should look for plugins.
- Add `dependencyResolutionManagement` block to define where the project should look for library dependencies.

## Verification Plan

### Automated Tests
- Run `./gradlew tasks` in the `android/` directory to verify that the build script can now be evaluated and plugins resolved.
