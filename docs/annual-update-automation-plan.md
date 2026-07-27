# Annual update automation plan

## Objective

Replace the annual checklist in the root README with a reproducible command that,
given a checked-out `iBurn-Data` revision and explicit annual metadata, produces
all reviewable Android changes, validates them, and can open an update pull
request from CI. The process must not need an attached Android device and must
not put the embargo unlock code or other secrets in Git or CI logs.

The intended operator experience is:

```shell
./gradlew annualUpdate \
  -PannualConfig=annual/2026.json \
  -PdataRevision=<iBurn-Data-commit>
```

The command should be safe to rerun: the same configuration and data revision
must result in the same tracked files and no working-tree diff after the first
run.

## Current manual steps and automation targets

| Manual responsibility | Automation target |
| --- | --- |
| Change `versionYear`, `versionCode`, and `versionName` in `iBurn/build.gradle` | Read one annual configuration file and generate Android build constants from it. Calculate the first release name as `<year>.1`; require an explicit override for later releases. |
| Edit event, camp/event embargo, art embargo, and mock dates in `EventInfo.kt` | Put ISO-8601 local dates/times in the annual configuration and generate a typed Kotlin object. Validate ordering and parse dates in the playa time zone. |
| Edit ignored `SECRETS.kt` | Keep it out of the update. Read `IBURN_UNLOCK_CODE` at build/runtime for staff builds, with a local-only fallback file; CI validates presence without printing the value. |
| Run `updateData` | Replace its imperative copies with an input/output-aware `Sync` pipeline that consumes the already checked-out submodule. Remove stale destination files and fail when required inputs are absent. |
| Decide whether to bump `MBTILES_VERSION` | Derive a stable map content digest and generate the installed-map identity from it, eliminating the manual counter. |
| Manually bump the bundled database filename | Derive its identity from the API JSON digest (or annual configuration plus digest), so a changed source always creates a new database asset name. |
| Attach a device and run `bootstrapDatabase` | Build the SQLite database on the host using a deterministic JVM/CLI generator that shares the app's parsing and schema rules. Keep the device task only as a parity check during migration. |
| Visually inspect and build | Add schema, asset, database, unit, and assemble checks. CI publishes a manifest and APK as workflow artifacts and creates a pull request only after checks pass. |

## Proposed design

### 1. Establish one versioned annual configuration

Add `annual/<year>.json` (or TOML/YAML if the project adopts a parser already in
the build) containing only non-secret inputs:

```json
{
  "year": 2026,
  "versionCode": 69,
  "versionName": "2026.1",
  "artEmbargo": "eventStart",
  "campEventEmbargo": "2026-08-26T00:00:00-07:00",
  "mockNow": "2026-08-31T10:05:00-07:00"
}
```

The dates above illustrate the schema rather than prescribing event dates. Event
start and end are read directly from the selected year's
`APIData/APIData.bundle/dates_info.json` in `iBurn-Data`; the operator must review
that upstream file before committing the configuration.

Make this file the source of truth for Gradle and generated Kotlin. Add an
`annualUpdateValidateConfig` task that rejects:

- a year that differs from the configuration filename or data directory;
- a non-increasing `versionCode` or a `versionName` with the wrong year;
- an upstream `eventEnd <= eventStart` or upstream dates for a different year;
- embargo dates after the event end;
- a mock date outside the scenario supported by tests; and
- unknown or missing keys, so typos cannot silently use defaults.

Generation should write under `build/generated/`, not mutate checked-in source.
Once consumers use generated values, remove annual literals from
`iBurn/build.gradle` and `EventInfo.kt`. Unit tests should verify boundary times
before, at, and after both embargo timestamps.

### 2. Make data synchronization strict, clean, and deterministic

Split `updateData` into Gradle tasks by asset family (`syncMap`, `syncGeocoder`,
`syncMedia`, and `syncApiJson`) and an aggregate `syncAnnualData`. Each task
declares the selected `iBurn-Data/data/<year>` paths as inputs and its Android
asset directory as an output.

Use `Sync`, rather than additive `copy`, so files removed upstream are removed
locally. Before changing destinations, validate the complete source contract:

- `geocoder/bundle.js`;
- `Map/Map.bundle/map.mbtiles`, both styles, camp GeoJSON, and glyphs;
- `APIData/APIData.bundle/{art,camp,event}.json`; and
- media files, which may be empty but must have a present bundle directory.

Move year-specific style rewrites out of inline replacements. Rewrites should
use year-independent patterns, parse JSON where practical, and validate that no
`iBurnData_iBurn<year>` or unresolved `{{mbtiles_path}}` reference remains.

The task should accept `-PdataRevision` only as an assertion. It must fail if
the submodule HEAD differs, or if the submodule has tracked/untracked changes;
it must never fetch or silently move the submodule. This separates acquisition
(the caller or CI checks out an exact commit) from transformation and prevents
an update from changing while it runs.

### 3. Replace counters with content identities

Generate `annual-manifest.json` from sorted relative paths, byte sizes, and
SHA-256 hashes. Record the annual config hash, submodule commit, tool/schema
version, and hashes for each output asset family. Do not include timestamps or
absolute paths.

Use a collision-resistant, Android-compatible value derived from the map digest
instead of manually editing `MBTILES_VERSION`. Use the API JSON digest in the
bundled database filename, for example
`playaDatabase2026-<12-hex-chars>.db`. Generate both values into the same Kotlin
source as the annual metadata. This guarantees cache invalidation when content
changes and stability when it does not.

### 4. Generate the database without a device

Extract JSON-to-database transformation from Android lifecycle and broadcast
code into a platform-neutral Kotlin module (preferred), or create a small JVM
tool using SQLite JDBC. The generator must:

1. create a new temporary database rather than update an old one;
2. apply the production Room schema and indexes;
3. parse the three synchronized JSON files through the same adapters/mapping
   functions used by the app;
4. run SQLite integrity and foreign-key checks;
5. verify row counts and stable identifiers against the source JSON; and
6. atomically move the result to the content-addressed asset path.

For initial rollout, generate once on the host and once through the existing
device task, normalize SQLite metadata, and compare schema, row counts, and
canonical query exports. Keep this parity test for at least one annual cycle;
then demote the device workflow to an optional smoke test.

Because database and JSON directories are currently ignored, decide explicitly
how the release obtains embargoed artifacts. Recommended: keep them untracked,
package them as access-controlled CI artifacts, and inject them only in the
restricted release job. The public update PR can still contain configuration,
submodule pointer, non-embargoed assets, and a manifest with sensitive entries
redacted. If private Git storage is preferred instead, narrow `.gitignore` and
document repository access controls before enabling the workflow.

### 5. Add validation gates

Create an aggregate `verifyAnnualUpdate` task that runs locally and in CI:

- configuration validation and generated-source compilation;
- JSON parsing plus required-field and unique-ID checks;
- SQLite `integrity_check`, `foreign_key_check`, schema comparison, and row-count
  reconciliation;
- MBTiles `integrity_check` and required metadata/table checks;
- JSON parsing of rewritten styles and existence checks for every local asset
  reference;
- media decode/header checks and detection of orphaned or duplicate media;
- a clean-rerun test (`annualUpdate`, capture hashes, rerun, compare hashes);
- unit tests, lint, and `assembleRegularDebug`; and
- a secret scan and assertion that ignored embargo data is not accidentally
  staged.

Emit a concise report containing counts, sizes, hashes, source commit, and
configuration values. Never print source JSON records, location data, or secret
environment values.

### 6. Orchestrate with CI and create the update PR

Add a manually dispatched workflow with `year`, `data_revision`, and optional
`version_name` inputs. Pin the Android/JDK environment and use recursive
submodule checkout at the requested commit. The workflow should:

1. reject a revision not reachable from the configured `iBurn-Data` remote;
2. run `annualUpdate` and `verifyAnnualUpdate`;
3. rerun generation to prove idempotence;
4. upload the report, manifest, database bundle, and debug APK with appropriate
   retention/access controls;
5. create or update a branch named `annual-update/<year>` and a pull request
   containing only allowed tracked outputs; and
6. put validation results and before/after asset counts in the PR body.

Protect the workflow environment so embargoed data and staff secrets require
reviewer approval. Grant the job minimal permissions (`contents: write` and
`pull-requests: write` only for the PR step), pin third-party actions by commit,
and separate the untrusted validation job from any job that can read secrets.
Do not schedule the workflow until data publication timing is reliable; manual
dispatch provides a safer first release.

## Implementation sequence

1. **Characterize the current output (implemented).** The
   `generateAnnualBaselineManifest` task now creates a deterministic inventory
   with fixture-based coverage, recording the selected data revision plus asset
   and database counts, sizes, extension counts, and hashes. The reviewed 2025
   baseline captures the public assets available in a normal checkout; its zero
   counts also make the absence of ignored embargoed JSON, media, and database
   artifacts explicit. Regenerate it with:

   ```shell
   ./gradlew :iBurn:generateAnnualBaselineManifest \
     -PannualManifestOutput=annual/baselines/2025.json
   ```
2. **Centralize metadata (implemented).** The newest `annual/<year>.json` is now
   the validated source of truth for build versions, embargoes, and the mock
   date. Event start/end come directly from that year's `iBurn-Data`
   `dates_info.json`. Gradle generates typed `AnnualMetadata` constants consumed
   by `EventInfo`, removing annual date and version literals from application
   code without changing asset production.
3. **Harden synchronization (implemented).** Strict validation now checks the
   clean submodule revision and complete source contract. Asset-family `Sync`
   tasks delete stale outputs, map rewriting is year-independent and validated,
   and `updateData` remains as a deprecated alias that never moves the submodule.
4. **Automate cache identities (implemented).** The generated map cache version
   and bundled database filename now come from SHA-256 content digests, so they
   remain stable for identical inputs and change without manual counters.
5. **Move database creation to the host (implemented; parity period active).** A
   deterministic Python/SQLite generator creates the read-only playa tables,
   reconciles stable IDs, and runs SQLite integrity checks. The app first creates
   its complete Room schema and then imports those tables. The old device task is
   retained only for parity testing during the first annual cycle.
6. **Add CI orchestration (implemented; enablement requires approval).** The
   manually dispatched workflow validates an exact private-data revision,
   proves idempotence, publishes restricted artifacts, and can create a draft
   update PR. Validation has read-only repository access; the separate PR job
   receives only a vetted patch and summary. Repository maintainers must
   configure and protect the `annual-update` environment before enabling it.
7. **Retire the manual path (implemented).** The README now documents the
   one-command workflow, verification, idempotence check, CI operation, and
   troubleshooting/recovery path.

Each phase should be its own reviewable pull request. Until phase 5 passes parity
testing, the existing device-generated database remains the release authority.

## Definition of done

The annual process is fully automated when all of the following are true:

- a maintainer supplies only reviewed annual metadata and an exact clean
  `iBurn-Data` commit;
- one command produces every non-secret app change and restricted release
  artifact without interactive prompts or a device;
- rerunning from a clean checkout produces byte-identical outputs;
- removed upstream assets cannot survive in the app bundle;
- map and database caches invalidate automatically exactly when their inputs
  change;
- malformed or incomplete data fails before a PR is created;
- CI verifies compilation, tests, asset references, database integrity, and
  absence of staged secrets/embargoed files;
- the generated PR identifies the data revision and summarizes all material
  changes for human approval; and
- rollback consists of selecting the prior config/data revision and rerunning
  the same workflow, not performing manual file edits.

## Explicit human approvals that remain

Automation should not decide official event/embargo dates, authorize publication
of embargoed location data, choose a staff unlock code, approve a version for
release, sign the production APK/AAB, or publish to the Play Store. Those are
security or product decisions. The workflow should make each approval visible
and auditable while automating all deterministic work around it.
