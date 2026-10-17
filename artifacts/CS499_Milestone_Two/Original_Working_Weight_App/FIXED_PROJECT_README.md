# Weight App - Repaired Data Grid

This copy preserves the original project structure and completes the missing
`DataGridActivity` behavior.

## Open and run

1. Extract the ZIP file.
2. In Android Studio, choose **File > Open**.
3. Select the `Weight_App_Fixed` folder containing `settings.gradle`.
4. Allow Gradle Sync to complete.
5. Run the `app` configuration on an emulator.
6. Create an account, log in, and test the Daily Weights screen.

## Working behavior

- Add a dated weight entry and immediately display it in the list.
- Load previously saved entries for the logged-in user.
- Tap an entry to edit it.
- Delete an entry.
- Save and reload a goal weight.
- Open SMS Settings.
- Send the existing emulator goal notification when an entered weight reaches
  the saved goal and SMS permission is enabled.
- Reject invalid dates and weights.
- Restrict updates and deletes to the logged-in user's records.

Use dates in `YYYY-MM-DD` format, such as `2026-09-08`.
