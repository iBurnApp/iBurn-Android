# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

iBurn is an Android application that provides an offline map and guide for the Burning Man art festival. The app displays camps, art installations, and events with both list and map views, featuring custom Black Rock City map tiles and full offline functionality.

## Key Commands

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build (unsigned)
./gradlew assembleRegularUnsigned

# Mock debug build (simulates being on-playa during the burn)
./gradlew assembleMockDebug

# Install debug build on connected device
./gradlew installRegularDebug
```

### Data Management
```bash
# Update playa data from iBurn-Data submodule
./gradlew updateData

# Bootstrap database (requires connected device)
./gradlew bootstrapDatabase
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Architecture Overview

### Core Components

**MainActivity** (`activity/MainActivity.java`): Entry point managing the UI with ViewPager + tabs structure. Handles initial setup including Google Play Services verification, data update scheduling, and onboarding flow.

**Fragments**:
- `MapboxMapFragment`: Interactive Black Rock City map using Mapbox
- `ExploreListViewFragment`: Shows current/upcoming events
- `BrowseListViewFragment`: Browse all camps, art, and events
- `FavoritesListViewFragment`: User's favorited items
- `SearchFragment`: Full-text search across all data

**Data Layer**:
- `DataProvider` (`database/DataProvider.kt`): Central database access point using Room
- `PlayaDatabase2` (`database/PlayaDatabase2.kt`): Room database configuration
- `DataUpdateService` (`service/DataUpdateService.kt`): Background service for data updates
- `IBurnService` (`api/IBurnService.java`): API client for fetching updates

**Key Models**:
- `Art`, `Camp`, `Event`: Core data entities with Room annotations
- `PlayaItemWithUserData`: Wrapper combining API data with user modifications (favorites, custom POIs)

### Important Configuration

**SECRETS.kt** (must be created manually):
```kotlin
package com.gaiagps.iburn

const val UNLOCK_CODE = "WHATEVER_PASSWORD"
const val IBURN_API_URL = "https://SOME_API"
const val MAPBOX_API_KEY = "YOUR_MAPBOX_KEY"
```

**EventInfo.kt**: Contains event dates and embargo information - must be updated annually.

**Version Configuration** (`iBurn/build.gradle`):
- `versionYear`: Current event year (e.g., 2025)
- `databaseName`: Versioned database filename
- `versionCode` and `versionName`: App version tracking

### Data Flow

1. Pre-bundled database ships in `assets/databases/`
2. On first launch, database is copied to app storage
3. `DataUpdateService` polls API for updates when gates open
4. User modifications (favorites, custom POIs) are preserved during updates
5. Map tiles are bundled as mbtiles in assets

### Annual Update Process

1. Update `versionYear` in `iBurn/build.gradle`
2. Update dates in `EventInfo.kt`
3. Update `UNLOCK_CODE` in SECRETS.kt
4. Run `./gradlew updateData` to pull latest data
5. Bump `MapboxMapFragment.MBTILES_VERSION` if map updated
6. Update `databaseName` version in build.gradle
7. Run `./gradlew bootstrapDatabase` to generate new database

### Development Notes

- The app uses a mix of Java and Kotlin
- Room database with FTS (Full Text Search) support
- RxJava for reactive programming patterns
- Mapbox for offline map rendering
- View binding is enabled for safer view references
- Mock build variant simulates on-playa conditions for testing