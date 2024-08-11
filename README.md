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

#### Update code and text resources

* Update `app_name` and `current_year` in iBurn/src/main/res/values/strings.xml
* Update `MOCK_NOW_DATE` in CurrentDateProvider (Used when simulating event time during testing)
* Update `versionYear`, `versionName` and `versionCode` in `iBurn/build.gradle`
* Update `EVENT_START_DATE` and `EVENT_END_DATE` in AdapterUtils.java
* Update `EMBARGO_DATE` in Embargo.java
* Update `UNLOCK_CODE` in SECRETS.kt

#### Art Images/Audio Tour

The art images and audio can be pulled from the iburn-Data library and should be put in `./assets/art_images` and `./assets/audio_tour` respectively.

#### Update playa data

1. Check out iBurn-Data into a directory adjacent to this repository's root, and point iBurn-Data to the branch appropriate for the current year. Note this will usually be the private repo.
2. `./gradlew updateData`. This will copy updated map, geocoder, art images, and api (camp, art, event) json files to this repo.
3. If the map.mbtiles were updated, bump `MapboxMapFragment.MBTILES_VERSION`
4 Update `DATABASE_NAME` in `PlayaDatabase2.kt` to represent the current year. Commit this change.
5. Make these temporary changes to app to generate a database file from JSON. Do not commit:
      *  Set `USE_BUNDLED_DB` to `false` in `PlayaDatabase2.kt`
      *  Uncomment the call to `bootstrapDatabaseFromJson` in `MainActivity`'s `onCreate`
6. Launch the app and confirm database bootstrap completion with logline `MainActivity: Bootstrap success: true`.
7. Copy the generated database from your device. Depending on the value of the database name
set in PlayaDatabase2.kt the file will be located somewhere like `/data/data/com.iburnapp.iburn3.debug/databases/playaDatabase2023.db`.
You can use Android Studio's "Device File Explorer" to conveniently copy this, or use `adb pull` from
the command line. Place the saved database in `iBurn/src/main/assets/databases`
8. return the value of `USE_BUNDLED_DB` in PlayaDatabase2.kt to `true`, Comment out call to `bootstrapDatabaseFromJson` in `MainActivity`'s `onCreate`

## TODO

* Pretty up that item detail view.
* Handle bundled database migrations so we allow app updates to use newer bundled data without using user modifications like favorites
* Investigate Mapbox offline issues. Seems like it's possible Mapbox gets into a state where it stops displaying the map

## Releasing
Make sure you've:

+ Set embargo date correctly in `Embargo.java`, and set `EVENT_START_DATE` and `EVENT_STOP_DATE` in `AdapterUtils`
+ Incremented the version code and name in ./iBurn/build.grade
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
