# Developing

iBurn allows you to browse a database of Burning Man Camps, Art, and Events within list and map views.
The map view includes a bespoke Black Rock City layer made by us.
The app includes a polling-based background data update service to keep itself up-to-date.

`MainActivity` handles most of the UI, and `DataUpdateService` manages the background data update.

You can build the special `mockDebug` build variant to simulate your test device
being on Playa during the Burn (see [`CurrentDataProvider.MOCK_NOW_DATE`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/345b8df09620b1fede7ad4649a068e7dc9f61982/iBurn/src/main/java/com/gaiagps/iburn/CurrentDateProvider.java#L15-L15) and [`LocationProvider.mockCurrentLocation`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/211c133f6ade747b2d9f0644972eac64f83fd0da/iBurn/src/main/java/com/gaiagps/iburn/location/LocationProvider.java#L97-L97)).

# Overview

[`MainActivity`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/master/iBurn/src/main/java/com/gaiagps/iburn/activity/MainActivity.java) is the app entry point.
In `onCreate` we perform initial app setup:

1. Verify Google Play Services is available (required by Google Maps)
2. Schedule the background data update if necessary.
3. Direct the user to onboarding ([`WelcomeActivity`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/master/iBurn/src/main/java/com/gaiagps/iburn/activity/WelcomeActivity.java)) if necessary.

The app UI is organized with a [ViewPager](https://developer.android.com/reference/android/support/v4/view/ViewPager.html) + tabs with the UI of each tab represented by a Fragment.
The [order](https://github.com/Burning-Man-Earth/iBurn-Android/blob/9968f1a0acb8d3a470123fc2ad0d3510d9587c47/iBurn/src/main/java/com/gaiagps/iburn/activity/MainActivity.java#L90...L97) and [content](https://github.com/Burning-Man-Earth/iBurn-Android/blob/9968f1a0acb8d3a470123fc2ad0d3510d9587c47/iBurn/src/main/java/com/gaiagps/iburn/activity/MainActivity.java#L301-L305) of each tab is defined in `MainActivity`.

The current content Fragments are:

 + `GoogleMapFragment` : BRC map
 + `ExploreListViewFragment`: Show events you can check out right now
 + `BrowseListViewFragment`: Browse Camps, Art, and Events as big boring lists
 + `FavoritesListViewFragment`: Browse Camps, Art, and Events you've favorited
 + `FeedbackFragment`: Provide feedback (currently by email, yuck!)

The content fragments are all self-contained and independent of `MainActivity`.

There's currently a persistent search Floating Action Button (yuck!) that
directs the user to [`SearchActivity`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/8cb6414628959b16531f108778781e7a8c51795b/iBurn/src/main/java/com/gaiagps/iburn/activity/SearchActivity.java).
Search is currently based on a name column query via [`DataProvider.observeNameQuery()`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/5f99b17e70c3080dc0780c74d7e7dc5933865293/iBurn/src/main/java/com/gaiagps/iburn/database/DataProvider.java#L305-L305),
but we should change to also incorporate descriptions.

[`PlayaItemViewActivity`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/44b30e369ef438df9899280c78688cbaed2a5d20/iBurn/src/main/java/com/gaiagps/iburn/activity/PlayaItemViewActivity.java) is used as a "detail view" of a Camp, Art, or Event
anywhere that's necessary in the app. It's also probably the most embarrassing piece of shit in the app because it
includes one big mess of view logic to adapt any item type into its view. I'd love to replace this with
specific view models for each item type.

# Data

All access to the database is performed through [`DataProvider`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/5f99b17e70c3080dc0780c74d7e7dc5933865293/iBurn/src/main/java/com/gaiagps/iburn/database/DataProvider.java).
Usually the app is built with a pre-bundled database in the `assets` directory. The
value of `DataProvider.USE_BUNDLED_DB` determines whether the app loads this pre-bundled
database.

All access to the map tiles are performed through [`MapProvider`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/a1d070833fc5522f9ad9d07d8ad60bbcda50ed2a/iBurn/src/main/java/com/gaiagps/iburn/database/MapProvider.java).
Map tiles are currently bundled as a `raw` resource, though we can move them to assets for consistency with the database.

[`IBurnService`](https://github.com/Burning-Man-Earth/iBurn-Android/blob/2bb84fc96ef259abafea65af35cce2c8aa3f212b/iBurn/src/main/java/com/gaiagps/iburn/api/IBurnService.java) contains the logic
for fetching database and map updates from our API and importing that data into the app.
The database update process is complicated by the fact that we allow the user to modify the database by
marking favorites and adding custom map markers. We could evaluate separating all user-made data into a separate
database from the API-supplied data. This would allow us to simplify the update process but would
complicate the querying. The current system has gone through two years of battle testing and
the data update mechanism is pretty critical so mayyybee  ¯\_(ツ)_/¯

