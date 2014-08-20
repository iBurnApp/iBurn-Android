# [iBurn-Android](https://github.com/Burning-Man-Earth/iBurn-Android)

iBurn is an offline map and guide for the [Burning Man](http://www.burningman.com) art festival.
The 2014 release has been refactored for Android 4.0.3+ (API 15) with improvements to Map performance, (hopefully) common-sense design and a bunch of cleanup.
We use [Schematic](https://github.com/SimonVT/schematic) to generate SQLiteOpenHelper and ContentProvider boilerplate.
For mapping we've stuck with Google's Maps API after thoroughly evaluating the Mapbox Android SDK. Maybe next year (along with vector tiles?).

For users of iOS devices, we also developed a completely re-written version of [iBurn for iOS](https://github.com/Burning-Man-Earth/iBurn-iOS).

[![iBurn Google Play Store Link](http://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=com.gaiagps.iburn&hl=en)

## Installation

* Make sure your Android SDK packages are up to date.
* `$ git clone https://github.com/Burning-Man-Earth/iBurn-Android --recursive`
* `$ cd ./iBurn-Android`
* `$ ./gradlew assembleDebug`

**Note**: Camp, Art and Event location data (`camps.json`, `art.json`, `events.json`) are embargoed by BMorg until the gates open each year. There isn't anything we can do about this until BMorg changes their policy. Sorry!

Fortunately, you can still run and test the app with the previous year's data.

## TODO

* Place POI at screen center.

* On-Boarder
* When searching map, smooth zoom to frame results
* Pretty up that item detail view.
* Show Favorites on the Map
* Scroll back to top when switching sort
* Put Playa location as first item in detail view (e.g. 7:45 & E)
* Put distance as second item in detail view
* Put description as third item in detail view
* Make map view on detail screen slightly smaller
* Show Playa location below camp/art relation link on Events detail view

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

© 2014 [David Brodsky](https://github.com/onlyinamerica)

Code: [MPL 2.0](https://www.mozilla.org/MPL/2.0/) (similar to the LGPL in terms of [copyleft](https://en.wikipedia.org/wiki/Copyleft) but more compatible with the App Store)

Data: [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/)