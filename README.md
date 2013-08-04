iBurn-Android
=============

iBurn for Android

## Map 
Currently using Google Maps V2 via the Google Play Services SDK. To swap the map with another provider, create a Fragment that initializes the desired map view on it's onCreate(). See GoogleMapFragment for an example.

Modify `setupFragmentStatePagerAdapter()` in MainActivity to connect your custom Map Fragment to the MAP TAB_TYPE.
