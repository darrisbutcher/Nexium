# Working on Nexium

The owner's standing preferences for Claude sessions on this repository.

## Pull requests and merging

- After pushing finished work, open a pull request into `main` and merge it once every CI check passes. Don't ask first.
- Only stop and tell the owner when something blocks this: a failing check that isn't a quick fix, a merge conflict,
  review comments, or a merge GitHub refuses.
- Merging a new `mod_version` publishes its GitHub Release automatically (the `release` job in
  `.github/workflows/build.yml`), so raise `mod_version` and add its `## <version>` section to `CHANGELOG.md` with each
  change that should ship. A version that already has a release publishes nothing.

## Deliverables

- Don't send the mod's `.jar` unless the owner asks for it.
- Show renders or previews of visual changes (textures, screens) and wait for approval before adding them to the mod.

## Checks before pushing

- `./gradlew build --no-daemon` runs the unit tests and every game test.
- `xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest --no-daemon` runs the client tests in a real game;
  screenshots land in `build/run/clientGameTest/screenshots`.
