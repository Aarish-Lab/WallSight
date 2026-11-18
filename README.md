# Dark_Venice's Wallsight (1.1.0) for Minecraft 26.1.2

Client-side Fabric mod: **cancel your own knockback** and an **ESP through walls** for players, hostile
mobs and chests, with **configurable colours**. Built for Minecraft 26.1.2, Fabric Loader 0.20.1,
Java 25 (the GitHub build is pinned to Java 25.0.1).

> **Status: built by GitHub, not yet run in the game.** Nothing here has been launched in Minecraft.
> See [What is and is not verified](#what-is-and-is-not-verified).

> **Server rules:** cancelling knockback and showing players through walls are prohibited on most
> multiplayer servers and are commonly detected and banned. Use it in singleplayer, on your own
> server, or where the rules allow it. This version does not change what either feature does.

## What changed from the 1.0.2 jar you uploaded

I decompiled `DarkVenice_Wallsight_26_1_2.jar` (no network, process or credential code in it) and kept
its knockback guard, mixin, keybind (Right Shift), ESP drawing and config file names exactly as they
were. Your message listed checks but no feature list, so I took the checks as the features to add:

- **Mod Menu integration** (new): a "configure" button that opens the settings screen and returns to Mod Menu.
- **ESP colours** (new): one colour each for players, hostile mobs and containers, stored in `config/wallsight.json`.
  Defaults are the exact colours the 1.0.2 jar hard-coded.
- **HEX input with validation** (new): type `#RRGGBB` (the `#` is optional, exactly six hex digits). Invalid text turns red
  and the last valid colour is kept.
- **Colour picker kept in sync** (new): HEX boxes, swatches and one shared picker update each other. Click a colour's
  name to choose which one the picker edits. "Reset colours" restores the defaults.
- **Scrolling settings panel** (new), so the longer screen stays usable at large GUI scales.
- Unchanged: master toggle (off by default), distance slider (8 to 128 blocks), the three ESP toggles, the keybind.

An existing `wallsight.json` from 1.0.2 still loads; the colours just default.

## Get the jar through GitHub (no installs needed)

1. Sign in at github.com, click **+** (top right) then **New repository**. Name it `Wallsight`, leave everything
   else untouched, click **Create repository**.
2. Unzip `Wallsight-repo.zip`. On the empty repo page click **uploading an existing file** and drag in everything
   from inside the unzipped `Wallsight` folder. Click **Commit changes**.
3. Check the repo page shows a `.github` folder. Browsers often skip it on drag and drop. If it is missing:
   **Add file, Create new file**, type `.github/workflows/build.yml` as the name (the slashes create the folders),
   paste in the contents of that file from the unzipped folder, **Commit changes**.
4. Click the **Actions** tab. The `build` run starts by itself (first run: several minutes). Yellow dot = running,
   green tick = done, red X = failed.
5. Click the run, scroll to **Artifacts** at the bottom, click **DarkVenice_Wallsight_26_1_2**. A zip downloads;
   `DarkVenice_Wallsight_26_1_2.jar` is inside. The run's summary page also shows the jar checks.
6. **Red X?** Open the failed step, copy the last ~40 lines of its log and send them over.
   If the error is that Java `25.0.1` cannot be found, change `'25.0.1'` to `'25'` in `.github/workflows/build.yml`.
7. Optional permanent link: `git tag v1.1.0 && git push origin v1.1.0` attaches the jar to a GitHub Release.

## Install

In `.minecraft/mods`: the new `DarkVenice_Wallsight_26_1_2.jar` (**delete the old jar of the same name**),
**owo-lib** 0.13.x for 26.1, **Fabric API** for 26.1.2, and optionally **Mod Menu** 18.x (and Sodium).
Needs Fabric Loader 0.20.1+ and Java 25. Right Shift (rebindable under Controls, "WallSight") opens the settings.

## What is and is not verified

Run here, by actually executing it (`bash run-selftest.sh`, 4705 checks, also run by GitHub before the build):

| Your check | Result |
|---|---|
| HEX validation | Tested: 6 digits, optional `#`, any case, rejects short/long/non-hex/non-ASCII digits/null; round trip over the colour range |
| Config persistence | Tested with the real config class and real Gson: save, load, overwrite, no temp files, a 1.0.2 file with no colours, out-of-range radius, bad/null colours, unknown fields, empty/broken/binary files |
| Colour-picker synchronisation | Tested on the sync model with widgets that echo like the real ones: hex to picker/swatch/config, picker to hex/swatch/config, selecting, reset, invalid text, and no event storms (every action ends within a few events) |
| Distance slider | Range maths tested (clamp 8 to 128, inclusive boundary, 3D distance, chunk scan covers the radius). The slider widget itself is unchanged from 1.0.2 and untested here |
| Fabric metadata, Java compatibility | `scripts/verify_jar.py` checks the built jar (id, client environment, Minecraft 26.1.2, Java 25+, Loader 0.20+, entrypoints and mixin classes exist, every class is Java 25 bytecode). It passed on a jar assembled from my compiled classes and resources, and it runs on the real jar in GitHub |
| Compile errors | All 18 source files compile with `-Xlint:all` and no warnings on JDK 25 (`--release 25`), **but against stand-in classes** I wrote from signatures in the original jar, owo-lib, Fabric API and Mod Menu sources. This already caught one real bug (fixed). The real compile happens on GitHub |

**Not verified (cannot be from here):**

- That Gradle resolves everything: owo-lib `0.13.1+26.1` from `maven.wispforest.io`, Mod Menu `18.0.2`, Loom, Fabric API.
- **Mod Menu integration** in the running game, and the **keybind** (code identical to 1.0.2, translation keys verified).
- **Everything visual:** the screen layout, scrolling, the owo colour picker and HEX box behaviour, swatches.
- **Player detection logic.** It is unchanged from 1.0.2: anything that is a `Player` other than yourself within the radius.
  That includes spectators, invisible players and the fake "players" some servers use as NPCs; I did not try to filter these.
- **Multiplayer client-side compatibility.** The mod is client-only (`environment: client`), its two mixins only touch
  client packet handlers, and nothing is sent to the server, but I have not run it on any server, and server-side
  anti-cheat can still flag or ban it.
- Java `25.0.1` exactly: the build asks for it, the JDK that runs the build is the only place that can confirm it.

## Layout

- `src/main/java/.../logic`, `config`: Minecraft-free logic and settings (tested).
- `src/main/java/.../esp`, `combat`, `input`, `mixin`, `ui`: Minecraft integration (carried over from 1.0.2 plus the new screen).
- `selftest/`, `selftest-stubs/`, `run-selftest.sh`: the tests. `scripts/verify_jar.py`: the jar checks.
- `.github/workflows/build.yml`: the build that makes the jar.
