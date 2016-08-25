# [iBurn-Android](https://github.com/Burning-Man-Earth/iBurn-Android)

iBurn is an offline map and guide for the [Burning Man](http://www.burningman.com) art festival.

Got iOS? You'll love [iBurn for iOS](https://github.com/Burning-Man-Earth/iBurn-iOS).

[![Google Play link](http://steverichey.github.io/google-play-badge-svg/img/en_get.svg)](https://play.google.com/store/apps/details?id=com.gaiagps.iburn)

## Installation

* Make sure your Android SDK packages are up to date.
* `$ git clone https://github.com/Burning-Man-Earth/iBurn-Android --recursive`
* `$ cd ./iBurn-Android`
* `$ touch ./iBurn/src/main/java/com/gaiagps/iburn/SECRETS.java && open ./iBurn/src/main/java/com/gaiagps/iburn/SECRETS.java`
* Copy the following into SECRETS.java:

    ```java
    package com.gaiagps.iburn;

    public class SECRETS {

        public static final String HOCKEY_ID = "YOUR_HOCKEYAPP_ID";
        public static final String UNLOCK_CODE = "WHATEVER";
        public static final String IBURN_API_URL = "SOME_URL";

    }
    ```
* `$ ./gradlew assembleDebug` or from Android Studio invoke 'Import Project' and select the `./iBurn-Android` directory.

**Note**: Camp, Art and Event location data (`camps.json`, `art.json`, `events.json`) are embargoed by the Burning Man Organization until the gates open each year. Sorry!

Fortunately, you can still run and test the app with the previous year's data.

## TODO

* When searching map, smooth zoom to frame results
* Pretty up that item detail view.
* Show Favorites on the Map
* Scroll back to top when switching sort
* Put Playa location as first item in detail view (e.g. 7:45 & E)
* Put distance as second item in detail view
* Put description as third item in detail view
* Make map view on detail screen slightly smaller
* Show Playa location below camp/art relation link on Events detail view


## Updating data
If bundled tiles are updated, you can change MapProvider.MBTILE_DESTINATION to force all upgrades to copy the bundled tiles.

Put bundled database in `./iBurn/main/assets/databases`, make sure DBWrapper filename is up to date, and bump version to force a dump-and-recopy.

## Releasing
Make sure you've:

+ Set embargo date correctly in `Embargo.java`, and adjust dates in `EventListViewFragment` and `AdapterUtils`
+ Incremented the version code and name in ./iBurn/build.grade
The final pre-signed store release should be built with:

    $ ./gradlew assembleRegularUnsigned

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