# [iBurn-Android](https://github.com/Burning-Man-Earth/iBurn-Android)

iBurn is an offline map and guide for the [Burning Man](http://www.burningman.com) art festival.

Got iOS? You'll love [iBurn for iOS](https://github.com/Burning-Man-Earth/iBurn-iOS).

[![Google Play link](http://steverichey.github.io/google-play-badge-svg/img/en_get.svg)](https://play.google.com/store/apps/details?id=com.gaiagps.iburn)

## Installation

* Make sure your Android SDK packages are up to date.
* `$ git clone https://github.com/Burning-Man-Earth/iBurn-Android --recursive`
* `$ cd ./iBurn-Android`
* `$ touch ./iBurn/src/main/java/com/gaiagps/iburn/SECRETS.kt && open ./iBurn/src/main/java/com/gaiagps/iburn/SECRETS.kt`
* Copy the following into `SECRETS.kt`:

    ```java
    package com.gaiagps.iburn

    const val UNLOCK_CODE = "WHATEVER_PASSWORD"
    const val IBURN_API_URL = "https://SOME_API"
    const val MAPBOX_API_KEY = "YOUR_MAPBOX_KEY"
    ```
* Copy the following into `./iBurn/fabric.properties`:

    apiKey=yourFabricApiKey

* `$ ./gradlew assembleDebug` or from Android Studio invoke 'Import Project' and select the `./iBurn-Android` directory.

**Note**: Camp, Art and Event location data (`camps.json`, `art.json`, `events.json`) are embargoed by the Burning Man Organization until the gates open each year. Sorry!

Fortunately, you can still run and test the app with the previous year's data.

## Annual Update

The current procedure is documented below. A staged design for replacing it with a
single, CI-friendly command is in the [annual update automation plan](docs/annual-update-automation-plan.md).

#### Update code and text resources

* Add `annual/<year>.json` with the version, embargo, and mock-date metadata.
  The newest annual configuration is selected automatically.
    * Increment `versionCode` by 1, and set `versionName` to "$year.1" for first release.
* Verify event start and end dates in
  `iBurn-Data/data/<year>/APIData/APIData.bundle/dates_info.json`. These dates are
  consumed directly; do not duplicate them in Android source.
* Update `UNLOCK_CODE` in SECRETS.kt


#### Update playa data

1. Check out the intended `iBurn-Data` commit and ensure the submodule is clean.
2. Run `./gradlew :iBurn:annualUpdate -PdataRevision=<full-commit-id>`.
   This validates the source contract, removes stale assets while synchronizing
   map, geocoder, media, and API JSON, and generates the bundled database on the
   host. No Android device is required.
3. Review the synchronized assets and generated database report. Map and
   database cache identities are derived from content and must not be bumped by
   hand.

#### Art Images/Audio Tour

The `annualUpdate` task synchronizes art images and tour audio from iBurn-Data to
`iBurn/src/main/assets/art_images` and `iBurn/src/main/assets/audio_tour`.

## TODO

* Pretty up that item detail view.
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
