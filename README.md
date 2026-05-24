# WaterMe

WaterMe is a native Android plant care reminder app built with Kotlin and Jetpack Compose.

## Features

- Add plants with a name, photo, type, location, and notes.
- Configure custom reminders for watering, fertilizing, repotting, misting, and pruning.
- See today's plant care tasks on the home screen.
- Browse upcoming tasks in a calendar-style agenda.
- Review each plant's reminders, care history, and health notes.
- Receive local notifications when plants need attention.

## Tech Stack

- Kotlin 2.3.21
- Jetpack Compose with Compose BOM 2026.05.00
- Material 3
- Android Gradle Plugin 8.13.0

## Build

Open the project in Android Studio, let it sync Gradle, then run the `app` configuration.

Command line builds require JDK 17 and Android SDK 36:

```powershell
gradle :app:assembleDebug
```
