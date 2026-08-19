# [iBurn-Android](https://github.com/Burning-Man-Earth/iBurn-Android)

iBurn is an offline map and guide for the [Burning Man](http://www.burningman.com) art festival.

Got iOS? You'll love [iBurn for iOS](https://github.com/Burning-Man-Earth/iBurn-iOS).

[![Google Play link](http://steverichey.github.io/google-play-badge-svg/img/en_get.svg)](https://play.google.com/store/apps/details?id=com.iburnapp.iburn3&hl=en)

## Installation

* Make sure your Android SDK packages are up to date.
* `$ git clone https://github.com/Burning-Man-Earth/iBurn-Android --recursive`
* `$ cd ./iBurn-Android`
* `$ ./gradlew assembleDebug` or from Android Studio invoke 'Import Project' and select the `./iBurn-Android` directory.

Optional local values can be supplied in untracked `~/.gradle/gradle.properties`:

```properties
iburnUnlockCode=staff-code
iburnApiUrl=https://example.invalid/
mapboxApiKey=optional-token
```

CI uses the equivalent `IBURN_UNLOCK_CODE`, `IBURN_API_URL`, and
`MAPBOX_API_KEY` environment variables. Values are compiled into the selected
artifact but are never written to generated source or logs.

**Note**: Camp, Art and Event location data are embargoed by the Burning Man
Organization until the gates open each year.

Fortunately, you can still run and test the app with the previous year's data.

## Annual Update

1. Review upstream event dates and create `annual/<year>.json`. Version codes
   must increase, first releases use `<year>.1`, and all timestamps must carry
   the correct `America/Los_Angeles` offset. Example:
   ```
    {
      "year": 2026,
      "versionCode": 70,
      "versionName": "2026.1",
      "locationEmbargo": "eventStart",
      "campAddressEmbargo": "eventStartMinusOneWeek",
      "mockNow": "2026-09-01T10:05:00-07:00"
    }
   ```
2. Check out the exact `iBurn-Data` commit. The selected `data/<year>` tree must
   be clean; automation never fetches or moves it.
3. Run:

   ```shell
   tools/annual-update.sh run \
     --year <year> \
     --data-revision <full-40-character-commit>
   ```

The update uses clean `Sync` outputs, generates the SQLite database on the
host, derives map/database cache identities from content, and writes the
manifest and verification report under `build/reports/annual-update/`.
Embargoed JSON, images, audio, and databases remain ignored and must be handled
only as restricted artifacts.

The script runs the update twice and verifies that `annual-manifest.json` is
unchanged before running the full verification suite. Run it with `--help` for
optional version assertions, an alternate config path, and CI-only checks.

The manual GitHub Actions workflow accepts a year and exact data revision. Run
it first in report-only mode. PR creation is an explicit input and stages only
the allowed configuration, submodule pointer, map, and geocoder outputs.

### Recovery

- A revision mismatch means the checked-out data does not match the asserted
  commit; select the intended revision and rerun.
- A source-contract or integrity failure must be fixed upstream rather than
  bypassed.
- Roll back by selecting the prior annual config and data revision, then run
  the same commands.
- Production signing and Play Store publication remain separate, explicit
  maintainer actions.

## TODO

* Investigate Mapbox offline and SIGABRT issues. Seems like it's possible Mapbox gets into a state where it stops displaying the map

## Releasing
Make sure you've:
* Verified event dates in the selected iBurn-Data `dates_info.json`
* Set the version code and name in `annual/<year>.json`
The final pre-signed store release should be built with:

```
    $ ./gradlew assembleRegularUnsigned
```

Pass the resulting apk off for signing. Then zipalign before publishing:

    $ zipalign -f -v 4 ./signed.apk ./signed-aligned.apk

## Contributing

Thank you for your interest in contributing to iBurn! Please open up an issue on our tracker before starting work on major interface or functionality changes. Otherwise, feel free to run wild!

1. Fork the project and do your work in a feature branch.
2. Make sure everything compiles and existing functionality is not broken.
3. Open a pull request.
4. Thank you! :)

Your contributions will need to be licensed to us under the [MPL 2.0](https://www.mozilla.org/MPL/2.0/) and will be distributed under the terms of the MPL 2.0.

## Authors

* [Chris Ballinger](https://github.com/chrisballinger) - iOS Development, Map Warping
* [David Chiles](https://github.com/davidchiles) - iOS Development, Map Styling
* [David Brodsky](https://github.com/onlyinamerica) - Android Development, Map Data
* [Savannah Henderson](https://github.com/savannahjune) - Map Styling

## Attribution

* [Andrew Johnstone](http://architecturalartsguild.com/about/) - Map Data (thank you!!)
* [Andrew Johnson](http://gaiagps.appspot.com/contact) - iBurn 2009-2013
* [Icons8](http://icons8.com) - Various icons used throughout the app.

## License

© 2016 [David Brodsky](https://github.com/onlyinamerica)

Code: [MPL 2.0](https://www.mozilla.org/MPL/2.0/) (similar to the LGPL in terms of [copyleft](https://en.wikipedia.org/wiki/Copyleft) but more compatible with the App Store)

Data: [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/)
