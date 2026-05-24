# WaterMe Reminder And Notification System

WaterMe uses WorkManager-backed local notifications for plant care reminders. Each concrete care task is scheduled as one `OneTimeWorkRequest` with a due timestamp. Recurring reminders are modeled by scheduling the next one-time request after the current task is completed or skipped.

## Android Constraints

WorkManager is durable and survives app restarts, but Android can defer work for battery and system health. For alarm-clock exact delivery, Android's exact alarm APIs would be required. WaterMe uses WorkManager here because the app's care reminders are durable background reminders rather than alarm-clock events.

## Core Classes

- `CareReminderSchedule`: serializable reminder payload for workers and notification actions.
- `ReminderScheduler`: schedules watering, fertilizing, repotting, misting, and pruning reminders.
- `CareReminderWorker`: displays the local notification when WorkManager runs the due task.
- `NotificationHelper`: creates channels, checks Android 13+ notification permission, and shows notifications.
- `ReminderActionReceiver`: receives notification action taps.
- `ReminderActionWorker`: handles Done, Snooze, and Skip actions in background work.
- `ReminderEventStore`: records lightweight completed, skipped, and snoozed action events for prototype reconciliation.
- `ReminderActionsViewModel`: example MVVM integration point for screen actions.

## Scheduling Flow

```text
Plant reminder created
  -> ReminderScheduler.scheduleReminder(...)
  -> WorkManager OneTimeWorkRequest
  -> CareReminderWorker
  -> NotificationHelper.showCareReminder(...)
```

## Notification Actions

```text
User taps Done
  -> ReminderActionReceiver
  -> ReminderActionWorker
  -> ReminderScheduler.markCompleted(...)
  -> record completed event
  -> cancel current work/notification
  -> schedule next recurring reminder

User taps Snooze
  -> ReminderActionReceiver
  -> ReminderActionWorker
  -> ReminderScheduler.snooze(...)
  -> record snoozed event
  -> replace work with new due timestamp

User taps Skip
  -> ReminderActionReceiver
  -> ReminderActionWorker
  -> ReminderScheduler.skip(...)
  -> record skipped event
  -> cancel current work/notification
  -> schedule next recurring reminder
```

## Android 13+ Permission Flow

The app declares `POST_NOTIFICATIONS`. Before posting a notification, `NotificationHelper` calls `NotificationPermissionHelper.canPostNotifications(context)`.

Screen-level flow:

```text
App starts or Settings opens
  -> if Android 13+ and permission not granted
  -> request POST_NOTIFICATIONS
  -> if granted, schedule pending reminders
  -> if denied, keep tasks visible but cancel notification work
```

## ViewModel Integration

`ReminderActionsViewModel` demonstrates how MVVM screens can call the scheduler:

- `scheduleWateringReminder(...)`
- `scheduleCareReminder(schedule)`
- `markTaskCompleted(schedule)`
- `skipTask(schedule)`
- `snoozeTask(schedule, minutes)`
- `updateNotificationPermissionResult(granted, pendingSchedules)`

When Room repositories are connected, ViewModels should update task and care history rows first, then call `ReminderScheduler` as the side effect.
