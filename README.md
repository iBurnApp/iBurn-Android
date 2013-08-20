iBurn-Android
=============

iBurn for Android

## Map 
Currently using Google Maps V2 via the Google Play Services SDK. To swap the map with another provider, create a Fragment that initializes the desired map view on it's onCreate(). See GoogleMapFragment for an example.

Modify `setupFragmentStatePagerAdapter()` in MainActivity to connect your custom Map Fragment to the MAP TAB_TYPE.

## TODO

+ Manage data embargo. Let's evaluate whether date / location means the embargo should be over on MainActivity's onCreate(), and set BurnState.embargoClear accordingly. Also add an action bar item to MainActivity to allow unlock via password.
+ Currently, on first app launch the device will load all JSON into the database, and then copy / prepare the map tiles. I'll reconfigure these actions to happen in parallel, so the map is immediately ready on first launch
+ Bundle database and copy on first launch. The app currently creates the database on first load from JSON
+ Show markers on map. Should we only show favorites? I think the map makes more sense as navigation view vs. discovery view.
+ Better organize the Event ListView. I wrote a sectionizer by date last year that I'll plop in at minimum.
