# CS 499 Milestone Two — Weight App

## What is included

- `Original_Working_Weight_App`: preserved working baseline used before these enhancements. Baseline functionality is not being presented as a new capstone enhancement.
- `Enhanced_Weight_App`: new software design/engineering version. Open this folder in Android Studio.
- `Historical_CS360_Source`: earlier CS 360 source, with generated build files and local settings omitted. It is separate from the working baseline.
- `CHANGES_AND_TESTS.md`: changes, tests, and verification status.

The enhanced app retains account creation/login, dated weight entries, row editing, deletion, goals, and optional SMS. The improvement is how responsibilities, state, security, and upgrades are handled.

## Open and run

1. Extract this ZIP to a new folder outside OneDrive, such as `G:\CS499\MilestoneTwo`. Do not overwrite your existing working project.
2. In Android Studio, choose **File > Open** and select `Enhanced_Weight_App`. Select the folder containing `settings.gradle`, not the outer submission folder or `app` alone.
3. Let Gradle sync finish. Use Android Studio's bundled JDK 17 or a compatible newer JDK, and install Android SDK 36 if prompted. This project uses AGP 8.13.2, Gradle 8.13, and Kotlin/Compose plugin 2.2.21.
4. Choose a disposable emulator with API 34 or newer. Press the green Run button for `app`.
5. Create a test account, then log in. Save a goal, add an entry, tap its row to edit it, and use Delete to remove it.

Do not uninstall or clear the app data on a device whose entries you want to keep. The enhanced app has the same application ID, so installing it on an existing device upgrades that installation. Test upgrades on a disposable emulator first. Do not reinstall version 1 over a migrated version 2 database.

## Run the tests

Open Android Studio's Terminal in `Enhanced_Weight_App`. On Windows run:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

The second command needs the emulator running. The database tests create and remove their own randomly named test databases, not `weight_tracker.db`. They do not send SMS.

Reports appear under `app/build/reports/tests/testDebugUnitTest` and `app/build/reports/androidTests/connected`.

## Project notes

The enhanced source includes the database-opening correction used for the manual functional checks. Generated build files and local Android Studio settings are omitted. CHANGES_AND_TESTS.md lists the completed manual checks and the automated tests that still need to run. The narrative is on the portfolio's Weight App page.
