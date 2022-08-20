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
* Update `versionName` and `versionCode` in `iBurn/build.gradle`
* Update `EVENT_START_DATE` and `EVENT_END_DATE` in AdapterUtils.java
* Update `EMBARGO_DATE` in Embargo.java

#### Update data

Follow instructions in "Working with mock data" below, placing art, camp, and event json
in iBurn/src/main/assets/json. Start app and observe data import succeeds with log line:
 `MainActivity: Update result success: true`.

Copy the generated database from your device. Depending on the value of the database name
set in PlayaDatabase2.kt the file will be located somewhere like `/data/data/com.iburnapp.iburn2.debug/databases/playaDatabase2018.db`.
You can use Android Studio's "Device File Explorer" to conveniently copy this, or use `adb pull` from
the command line. Place the saved database in `iBurn/src/main/assets/databases`, and toggle
the value of `USE_BUNDLED_DB` in PlayaDatabase2.kt back to true.

## TODO

* Pretty up that item detail view.
* Handle bundled database migrations so we allow app updates to use newer bundled data without using user modifications like favorites
* Investigate Mapbox offline issues. Seems like it's possible Mapbox gets into a state where it stops displaying the map

## Updating data
If bundled tiles are updated, you can change MapProvider.MBTILE_DESTINATION to force all upgrades to copy the bundled tiles.

Put bundled database in `./iBurn/main/assets/databases`, make sure PlayaDatabase2.kt filename is up to date, and bump version to force a dump-and-recopy.

## Working with mock data

First, disable using any bundled db by changing `USE_BUNDLED_DB` to false in PlayaDatabase2.kt
Place `art.json`, `camp.json`, `event.json` in ./iBurn/src/main/assets/json, and add
the following code somewhere on app start, like MainActivity#onCreate ~L#130:

```java
Context context = getApplicationContext();
IBurnService service = new IBurnService(context, new MockIBurnApi(context));
service.updateData().subscribe(success -> Timber.d("Update result success: %b", success));
```

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