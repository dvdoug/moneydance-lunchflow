# Local Moneydance build jars

These files are **not** in git. `gradlew fetchMdJars` (also a dependency of compile) downloads them from [moneydance_open](https://github.com/TheInfiniteKind/moneydance_open) `lib/` if missing. You can still copy them by hand:

| File | From |
| --- | --- |
| `moneydance-dev.jar` | [Moneydance DevKit](https://infinitekind.com/developer) `lib/` or [moneydance_open](https://github.com/TheInfiniteKind/moneydance_open) `lib/` |
| `extadmin.jar` | Same `lib/` (needed to generate keys and sign the `.mxt`) |
| `kotlin-stdlib-1.9.21.jar` | Same `lib/` (KeyAdmin/signing; compile uses Maven) |

They are already present on this machine if you ran the Phase 1 setup.
