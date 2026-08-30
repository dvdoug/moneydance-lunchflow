# Contributing

## Before writing code

Read [AGENTS.md](AGENTS.md) (especially **Current state**) and [docs/product.md](docs/product.md). The Gradle project already exists. Next work is Phase 4 polish (Help 101, screenshots, cancel, icon) unless the owner asks otherwise. Do not re-scaffold the DevKit layout. Do not implement [docs/review-v19-txn-types.md](docs/review-v19-txn-types.md).

## Conventions

- Kotlin, package `com.moneydance.modules.features.lunchflow`.
- Java 17 bytecode, Kotlin 1.9 language/API, matching current Infinite Kind guidance.
- Swing on the EDT; HTTP off it.
- No hardcoded Lunch Flow keys. Personal API only.

## Java / Kotlin toolchain

You do not set `CLASSPATH`. You do not install a Kotlin SDK.

A JDK is required only so the Gradle wrapper can run. This repo targets **Microsoft OpenJDK 21** (`winget install Microsoft.OpenJDK.21`). After that, `./gradlew` pulls the Kotlin compiler itself.

Moneydance ships its own JRE; the extension runs inside that, not on whatever JDK compiled it.

## Test data

Prefer the throwaway file **Lunch Flow testing**. The owner may also alpha the main file after **File → Export Backup**. Agents must not request production data or API keys.

Moneydance reopens the **last** file. After a throwaway session, **File → Open** the real dataset before closing if that is what they want next.

The key goes in extension Settings only. Auto-import on file open is default on once a file has a key and mappings; uncheck **Import when this file opens** until mappings are right.

## Installing a new build

Moneydance copies the `.mxt` into `%USERPROFILE%\.moneydance\fmodules`. It does not watch `dist/`. After each Gradle build, drag `dist/lunchflow.mxt` onto the Moneydance window (or **Extensions → Manage Extensions → Add from File…**) and Install. Same `id` replaces the previous build. Restarting without that step does not pick up new code.

## Local secrets

Drop DevKit jars into `lib/` as documented once the build exists. Generate signing keys with the DevKit `genkeys` task. Those files are gitignored.

Use a **throwaway** Moneydance data file and a Lunch Flow API destination you can rotate.

## Docs

If you change behaviour, IDs, or commands, update `AGENTS.md` and the matching file under `docs/` in the same change. Keep `CLAUDE.md`, `GEMINI.md`, and `.github/copilot-instructions.md` as pointers, not a second copy of the rules.

Every shipped `module_build` bump must add 1–2 high-level lines to [CHANGELOG.md](CHANGELOG.md) (Keep a Changelog). Commit **and push** each iteration, including docs-only work; do not leave the tree uncommitted or only local.

## GitHub Actions CI

Moneydance is a desktop app. CI **cannot** open a data file or click Import. It runs `./gradlew test` (API parser, FITIDs, dates, settings) on every pull request and push.

On **master** it also packages an **unsigned** `.mxt` (compiled classes + `meta_info.dict`, like a Phar, not a source tarball).

- **Workflow artifact** (Actions → run → Artifacts): convenient, expires in 90 days.
- **GitHub Release** on tag `v{module_build}`: the lasting download. CI creates the tag and Release the first time that build number hits master; it will not move or replace an existing tag (module_build is irrevocable). Changelog text for that version is the release notes.

Moneydance will warn that the signature is unrecognized until Infinite Kind counter-signs a store build. Local `gradlew lunchflow` still signs with your gitignored key.

DevKit jars are not in git. CI (and a first clone) run `fetchMdJars`, which pulls `moneydance-dev.jar` / `extadmin.jar` from [moneydance_open](https://github.com/TheInfiniteKind/moneydance_open).
