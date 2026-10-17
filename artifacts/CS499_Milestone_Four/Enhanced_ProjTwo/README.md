# CS 499 Milestone Four — Database Enhancement
CJ Mavrogeorge

## Start the sample dashboard (Windows PowerShell)
Open a terminal in Enhanced_ProjTwo. Python 3.11 or newer is recommended.

```powershell
py -3 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
.\.venv\Scripts\python.exe dashboard.py --demo
```
Open http://127.0.0.1:8050. Stop with Ctrl+C. On macOS/Linux use python3 to create
the environment and .venv/bin/python for subsequent commands.
The demo uses 80 synthetic records in memory. Changes disappear on exit. It does
not prove a connection to the AAC database. Map tiles require internet access.

## Use an existing MongoDB AAC database
Set these environment variables in the same PowerShell window (use your actual
connection URI and database/collection). The example below targets a local server.
This program does not automatically load .env files.

```powershell
$env:MONGODB_URI = 'mongodb://localhost:27017'
$env:MONGODB_DATABASE = 'aac'
$env:MONGODB_COLLECTION = 'animals'
.\.venv\Scripts\python.exe dashboard.py
```
Use a read-only account for viewing and appropriate separate permissions for
maintenance. Credentials belong in environment settings, never source control.
The original AAC dataset, MongoDB server, and historical Apporto service are not
included. Original_ProjTwo preserves the original code with credentials removed and
saved notebook outputs cleared. The enhanced application does not import it.

## Index setup and measurement
Creating an index changes database metadata. Run it intentionally on your own
configured database using a maintenance account:

```powershell
.\.venv\Scripts\python.exe setup_indexes.py
.\.venv\Scripts\python.exe benchmark.py
```
benchmark_results.json compares a forced collection scan with the named compound
index for the same water-rescue query. It records returned records, examined
records/keys, and execution milliseconds. This is a controlled plan comparison,
not a full dashboard benchmark. Compare equal nReturned first. Run several times
on representative data; caching and dataset size affect timings. No performance
improvement has been measured in the delivered environment. The breed regex is a
residual filter; the compound index targets animal type, sex, and age. Pagination
uses bounded limit/skip; very deep pages still require extra server work.

## Optional live integration test
TEST_MONGODB_URI enables a test which creates a randomly named cs499_test_ database
and removes that same database in a finally block. Use a development server and an
account allowed to create/drop test databases. It never uses MONGODB_DATABASE.

```powershell
$env:TEST_MONGODB_URI = 'mongodb://localhost:27017'
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
Remove-Item Env:TEST_MONGODB_URI
```
Without that variable the live test is explicitly skipped. Mock tests verify
application behavior, not server authentication, network behavior, or query plans.

## Files and behavior
- animal_shelter.py: validated CRUD, environment settings, bounded reads, named
  projection, aggregation, compound index and sanitized database exceptions.
- dashboard.py: standalone Dash dashboard, rescue filters, 10-row server pages,
  top-20 breed chart, and stable-record-ID map selection.
- demo_data.py: labeled synthetic data; no actual shelter records.
- tests/: mock-based behavior tests and opt-in real MongoDB test.
- benchmark.py and setup_indexes.py: explain comparison and explicit index setup.
- ProjectTwoDashboard_Enhanced.ipynb: optional notebook launcher.

Create returns an ID string; update/delete return affected-record counts. Empty
results are distinct from database failures. Update/delete require one MongoDB
record ID and cannot receive bulk queries. Writes accept only named fields and
validate values. An animal ID is not unique: multiple outcome records may refer
to the same animal. Coordinates must be provided together. The dashboard is
read-only; CRUD methods are available to Python callers. Logs include operation
and outcome fields but do not include records or credentials.

## Manual verification still needed
Run against your actual MongoDB server, capture explain results, open the dashboard
in a browser, try all filters and both page buttons, select map rows, and check
empty/missing-coordinate behavior with representative data. Confirm authentication
and permissions. Read the included automated test report for completed checks.

## Design tradeoffs
The original notebook loads all records into pandas. The enhancement moves
filtering, counts and grouping to MongoDB, and transfers only the requested page.
The chart shows the top 20 breeds and rescue categories may overlap. There is no
claim that all data is secure or that every failure mode is tested. An authenticated
multi-user deployment and cursor pagination would be future improvements. Dash
DataTable is pinned for this project; it emits a future-removal warning and can be
replaced with AG Grid during a future dependency upgrade.
