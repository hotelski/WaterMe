# WaterMe Navigation And User Flow

WaterMe uses a single-activity Navigation Compose setup with MVVM screen boundaries. ViewModels own each screen's UI state and user actions; the navigation layer maps screen events and one-off ViewModel effects to route changes.

## Routes

| Screen | Route | Arguments |
| --- | --- | --- |
| Onboarding | `onboarding` | None |
| Today | `today` | None |
| My Plants | `plants` | None |
| Add Plant | `plants/add` | None |
| Plant Details | `plants/{plantId}` | `plantId` |
| Edit Plant | `plants/{plantId}/edit` | `plantId` |
| Reminder Setup | `reminders/setup?plantId={plantId}` | Optional `plantId` |
| Calendar | `calendar` | None |
| Care History | `care-history?plantId={plantId}` | Optional `plantId` |
| Settings | `settings` | None |

## Bottom Navigation

Bottom navigation appears only on top-level destinations:

- `Today`
- `Plants`
- `Calendar`
- `Settings`

Nested screens do not show the bottom bar:

- Add Plant
- Plant Details
- Edit Plant
- Reminder Setup
- Care History
- Onboarding

## Screen Transitions

The graph uses short fade transitions for enter, exit, pop enter, and pop exit. This keeps plant-care flows calm and avoids heavy movement on small Android screens.

## Onboarding Flow

```text
onboarding
  Start caring for plants
    -> today
    -> remove onboarding from back stack
```

Back behavior:

- After onboarding completes, Android back should not return to onboarding.
- Settings can open onboarding again without deleting data.

## Add Plant Flow

```text
today or plants
  Add
    -> plants/add
  Save plant
    -> reminders/setup
  Save reminders
    -> plants
```

Production MVVM behavior:

- `AddPlantViewModel` saves the plant and emits `PlantSaved(plantId)`.
- Navigation calls `WaterMeNavigationActions.onPlantSaved(plantId)`.
- That opens `plants/{plantId}` and removes `plants/add` from the stack.

Back behavior:

- Back from Add Plant returns to the previous screen.
- Back from Reminder Setup returns to Add Plant or Edit Plant.

## Edit Plant Flow

```text
plants/{plantId}
  Edit
    -> plants/{plantId}/edit
  Save changes
    -> plants/{plantId}
```

Reminder setup from edit:

```text
plants/{plantId}/edit
  edit reminder schedule
    -> reminders/setup?plantId={plantId}
  Save reminders
    -> plants/{plantId}
```

Back behavior:

- Back from Edit Plant returns to Plant Details.
- After save, Edit Plant is removed from the stack.

## Calendar Task Flow

```text
calendar
  tap task
    -> plants/{plantId}
```

Back behavior:

- Back from Plant Details returns to Calendar.
- Completing a task from Calendar does not navigate; the ViewModel updates task state.

## Care History Flow

```text
plants/{plantId}
  View all history
    -> care-history?plantId={plantId}
```

Global history can also use:

```text
care-history
```

Back behavior:

- Back from Care History returns to the source screen.

## Settings Flow

```text
settings
  Show onboarding again
    -> onboarding
```

Settings actions that do not navigate:

- Notification permission review
- Notifications enabled toggle
- Dark mode preference
- Measurement units
- Backup/sync toggle

## Kotlin Implementation

Navigation code lives in:

- `WaterMeRoutes.kt`
- `WaterMeNavigationActions.kt`
- `BottomNavigationBar.kt`
- `AppNavGraph.kt`
- `WaterMeNavigationScaffold.kt`

The graph includes example event-to-navigation calls for every screen. When concrete ViewModels are connected, keep the same event mapping and replace preview UI states with `StateFlow` values collected from each screen ViewModel.
