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

## Design

The full mobile UI/UX flow, screen copy, navigation model, empty states, colors, and actions are defined in [docs/ui-ux-flow.md](docs/ui-ux-flow.md).

## Data And Logic

The Room database schema, relationships, DAO examples, reminder rules, notification scheduling, care history, and settings logic are defined in [docs/database-and-app-logic.md](docs/database-and-app-logic.md).

## Architecture

The recommended Kotlin, Jetpack Compose, MVVM, Room, WorkManager, Hilt, Navigation Compose, and Material 3 architecture is defined in [docs/android-architecture.md](docs/android-architecture.md).

## Navigation

Navigation Compose routes, bottom navigation, screen transitions, argument passing, and example screen event navigation calls are implemented in `app/src/main/java/com/hotelski/waterme/navigation`.
The full navigation and back-stack user flow is documented in [docs/navigation-user-flow.md](docs/navigation-user-flow.md).

## Reminders And Notifications

The WorkManager reminder scheduler, notification helper, worker classes, snooze/skip/complete actions, Android 13+ permission checks, and ViewModel integration example are implemented in `app/src/main/java/com/hotelski/waterme/notifications` and documented in [docs/reminder-notification-system.md](docs/reminder-notification-system.md).
