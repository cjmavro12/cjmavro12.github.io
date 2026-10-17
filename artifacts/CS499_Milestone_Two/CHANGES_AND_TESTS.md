# Changes and verification

## Source changes

| Area | Enhanced files | What changed |
| --- | --- | --- |
| Screen responsibilities | DataGridActivity, LoginActivity, WeightViewModel, LoginViewModel | Activities show data and forward actions. ViewModels own screen state. Coroutine IO work keeps SQLite and password hashing off the UI thread. ViewModels survive rotation, not process death. |
| Data access | WeightRepository, AppDatabaseHelper | One repository boundary validates values and scopes reads/updates/deletes to the signed-in user. Cursors/helpers are closed. |
| Passwords | PasswordHasher, AppDatabaseHelper | PBKDF2-HMAC-SHA256 with 600,000 iterations, random 16-byte salt, 256-bit hash, encoded version/parameters, constant-time digest comparison. |
| Upgrade | AppDatabaseHelper | Version 1 to 2 transaction hashes existing passwords in place, keeps record IDs, weights, and goals, and creates a user/date index. No drop-and-recreate upgrade. |
| Validation | InputValidator | Real calendar dates and finite weights/goals from 1 through 2000. Same rules apply to adds and edits. |
| List refresh | WeightAdapter | ListAdapter and DiffUtil compare stable row IDs and content instead of redrawing the entire list with notifyDataSetChanged. |
| SMS | GoalNotifier, SmsPermissionActivity, activity_sms_permission.xml | Sending is separate from screens, with account-specific opt-in, editable destination, disable control, and denied-permission handling. No weight value is included in the outgoing text. |
| Backup privacy | AndroidManifest, backup_rules, data_extraction_rules | App backups and transfers of private databases/preferences excluded. This is not database encryption. |
| Build | app/build.gradle, gradle/libs.versions.toml | Adds ViewModel and LiveData dependencies; aligns Kotlin/Compose plugins at 2.2.21; application version becomes 2. |

## Automated tests added

`EnhancementUnitTest` checks dates, leap years, invalid/nonfinite weights, random salts, correct/wrong passwords, malformed hash records, and DiffUtil identity/content comparisons.

`DatabaseEnhancementTest` checks the version 1 to 2 migration, password conversion, preserved IDs/weights/goals, reopening the migrated database, duplicate usernames, repository CRUD, invalid dates, and rejection of cross-user update/delete attempts.

Template tests remain as historical scaffolding; the new tests are the milestone coverage. No claim of full coverage is made. ViewModel rotation and SMS flows are covered by the manual checklist below, not these automated tests.

## Confirmed functional checks

After correcting the database-opening issue, I checked account creation/login, adding and editing entries, goal saving, reopening saved data, deleting entries, input validation, and opening SMS settings with permission handling. These were manual checks. The automated tests still need to be run.

Automated unit/instrumented tests, migration from a version 1 installation, rotation, cross-account isolation, and actual SMS delivery have not been confirmed. The uploaded working source is preserved unchanged. Duplicate-account attempts still perform password hashing before the unique constraint rejects the username, so they may take time.

## Manual checklist on a disposable emulator

Record PASS/FAIL and screenshots for each item after testing.

1. Create a test account. Duplicate username is rejected; wrong password cannot log in.
2. Add `2026-09-20` / `190`. Row appears once. Add a second dated row and confirm date ordering.
3. Tap a row and change its weight; only the changed content updates. Delete one row and confirm the other remains.
4. Try `2026-02-30`, a blank date, zero/negative weights, and values above 2000. Each is rejected without inserting data.
5. Save a goal of `170`; close/reopen and confirm the goal and remaining entries are retained.
6. Rotate the emulator during normal use and during a save. Confirm no duplicate save, crash, or missing saved records. Saved data reloads after process restart; in-flight operations are not guaranteed across process death.
7. Close/reopen and log in as another test account. Its weights/goals must be separate. Previous account's SMS opt-in must not carry over.
8. Deny SMS permission and continue adding/editing/deleting weights. Disable SMS and verify that no SMS is requested on a qualifying save.
9. Optional SMS test: use only a destination you control on a test emulator/device. Enabling SMS can incur carrier charges. A save at/below the goal requests a message; the app reports submission, not confirmed delivery. Each qualifying add/edit may send another message. Do not test with another person's number.
10. Upgrade rehearsal: on a separate disposable emulator install the baseline, create an account and sample records/goal, then install the enhanced app using the same signing key WITHOUT uninstalling. Confirm the old password logs in and records remain. First upgrade/login may take longer while legacy accounts are hashed. If signing differs, use a separate test emulator rather than uninstalling your real data.

## Limits and tradeoffs

The migration is one-way; do not downgrade the database. Secure-delete and backup exclusions reduce exposure but cannot erase old backups, snapshots, or guarantee forensic erasure of flash storage. Weight data remains in Android's private SQLite storage without additional encryption. This local classroom app is not a production authentication service. New-account username/password length limits are 80/128; login preserves compatibility with longer legacy credentials. Password hashing makes login slower by design; measure latency on your emulator. All rows still load into memory, so pagination remains future work. SMS delivery depends on device/SIM/service and has no delivery receipt or deduplication. The automated tests do not send messages.

## References

Android Developers. ViewModel overview. https://developer.android.com/topic/libraries/architecture/viewmodel

OWASP Foundation. Password Storage Cheat Sheet. https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
