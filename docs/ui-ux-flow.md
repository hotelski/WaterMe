# WaterMe UI/UX Flow

This document defines the complete mobile-first Android UI/UX flow for WaterMe. The app uses a clean Material Design style with soft green as the primary color and white, beige, and light gray as supporting colors.

## Design System

### Color Palette

| Token | Hex | Use |
| --- | --- | --- |
| Primary Soft Green | `#79B987` | Selected states, friendly highlights, secondary action surfaces |
| Primary Action Green | `#2F7D4B` | Main buttons, important icons, active navigation |
| Background Green White | `#F6FBF7` | App background |
| Surface White | `#FFFFFF` | Cards, sheets, input surfaces, navigation bar |
| Warm Beige | `#FFFBF3` | Empty states, calm helper panels |
| Light Gray | `#E7F1EA` | Dividers, input outlines, muted surfaces |
| Mist Blue | `#7FB9D7` | Watering and misting accents |
| Clay Beige | `#B88C4A` | Repotting, attention notes, warm status accents |
| Text Ink | `#173326` | Primary text |
| Muted Text | `#5E7267` | Secondary text |

### Typography

- Use Material 3 default Android typography.
- Screen titles: `Title Large`, semibold.
- Card titles: `Title Medium`, bold.
- Body text: `Body Medium`.
- Metadata and labels: `Label Medium` or `Body Small`.
- Keep text left-aligned except onboarding, where the main intro can be centered.

### Shape, Spacing, And Elevation

- App background: full-screen `Background Green White`.
- Cards: rounded rectangles, 20-28 dp corner radius.
- Buttons: 16-18 dp corner radius.
- Inputs: Material outlined text fields.
- Screen padding: 20 dp horizontal.
- Card internal padding: 16-20 dp.
- Vertical rhythm: 12-16 dp between cards.
- Elevation: low, soft shadows only. Main summary cards use slightly higher elevation than regular cards.

### Icons

Use Material Icons Rounded.

| UI Element | Icon |
| --- | --- |
| Today navigation | `Home` |
| My Plants navigation | `LocalFlorist` |
| Calendar navigation | `Event` |
| Settings navigation | `Settings` |
| Add plant | `Add` |
| Back | `ArrowBack` |
| Complete task | `Check` |
| Open details | `ChevronRight` |
| Choose photo | `PhotoCamera` |
| Notifications | `Notifications` |
| App/logo fallback | `LocalFlorist` |

Care task glyphs use short letter badges inside tinted rounded squares:

| Reminder | Label | Badge | Accent |
| --- | --- | --- | --- |
| Watering | `Watering` | `W` | Mist Blue |
| Fertilizing | `Fertilizing` | `F` | Primary Soft Green |
| Repotting | `Repotting` | `R` | Clay Beige |
| Misting | `Misting` | `M` | Teal Green |
| Pruning | `Pruning` | `P` | Primary Action Green |

## Navigation Model

### First Launch

1. User opens WaterMe.
2. If onboarding has not been completed, show Onboarding.
3. User taps `Start caring for plants`.
4. Navigate to Today.

### Primary Navigation

Bottom navigation appears on main screens only.

Menu items:

- `Today`
- `Plants`
- `Calendar`
- `Settings`

Bottom navigation behavior:

- `Today` opens the home screen with tasks due today.
- `Plants` opens the plant collection.
- `Calendar` opens upcoming tasks grouped by date.
- `Settings` opens app preferences and stats.
- The selected item uses Primary Action Green for icon and label.
- Unselected items use Muted Text.

### Secondary Navigation

- Floating action button on `Today` and `Plants`: opens Add Plant.
- Plant card tap: opens Plant Details.
- Back button on Add Plant: returns to previous main screen without saving.
- Back button on Plant Details: returns to the previous main screen.
- Completing a task keeps the user on the same screen and updates reminders immediately.

## Screen 1: Onboarding

### Purpose

Introduce WaterMe as a personal assistant for keeping houseplants alive and healthy.

### Layout

- Full-screen Background Green White.
- Centered content with 28 dp padding.
- Large rounded app logo tile at top.
- App name below logo.
- Short intro text.
- Four compact feature pills in a two-by-two grid.
- Primary button anchored visually near the bottom of the content stack.

### Exact Content

Title:

`WaterMe`

Intro:

`A calm plant care assistant for watering, feeding, pruning, repotting, and spotting small health changes before they become big ones.`

Feature pills:

- `Care tasks`
- `Health notes`
- `Calendar`
- `Notifications`

Primary button:

`Start caring for plants`

### Buttons And Actions

- `Start caring for plants`: marks onboarding complete and navigates to Today.

### Empty State

None. Onboarding always has content.

## Screen 2: Today

### Purpose

Show the plants that need care today and make logging care fast.

### Layout

- Top app bar title: `Today`
- Main summary card at the top.
- Section title: `Today's Care`
- Due task cards.
- Section title: `Health Notes`
- Recent health note cards.
- Bottom navigation.
- Floating action button with `Add` icon.

### Summary Card

When tasks are due:

Title format:

`{count} care task today`

Plural title format:

`{count} care tasks today`

Subtitle:

`A few plants need attention. Log care as you go and WaterMe will move the schedule forward.`

When no tasks are due:

Title:

`All clear today`

Subtitle:

`Your plants are on schedule. Check the calendar for what is coming next.`

Metric pills:

- `{plantCount} plants`
- `{reminderCount} reminders`

### Task Card Layout

Each care task card includes:

- Care type glyph.
- Care type label.
- Plant name.
- Plant location.
- `Done` tonal button with `Check` icon.

Reminder labels:

- `Watering`
- `Fertilizing`
- `Repotting`
- `Misting`
- `Pruning`

### Buttons And Actions

- Floating action button: opens Add Plant.
- Task card tap: opens Plant Details.
- `Done`: logs the care action, adds a care history entry, and reschedules the reminder.

### Empty States

Today's Care empty title:

`No urgent care`

Today's Care empty message:

`Enjoy the quiet. Your next care task is waiting in the calendar.`

Health Notes empty title:

`No notes yet`

Health Notes empty message:

`Use plant details to track yellow leaves, dry soil, fresh growth, or anything worth remembering.`

## Screen 3: My Plants

### Purpose

Let users browse all plants and quickly see care status.

### Layout

- Top app bar title: `Plants`
- Header row:
  - Left: plant count and helper text.
  - Right: `Add` tonal button.
- Vertical list of plant cards.
- Bottom navigation.
- Floating action button with `Add` icon.

### Exact Content

Header title format:

`{plantCount} plants`

Header helper:

`Care schedules, notes, and photos in one place.`

Add button:

`Add`

### Plant Card Layout

Each plant card includes:

- Plant photo or fallback leaf icon.
- Plant name.
- Plant type and location.
- Due badge when care is due:
  - `{count} due`
- Next reminder line:
  - `Next: {shortReminderLabel} {date}`
- Open details chevron.

Short reminder labels:

- `Water`
- `Feed`
- `Repot`
- `Mist`
- `Prune`

### Buttons And Actions

- `Add`: opens Add Plant.
- Floating action button: opens Add Plant.
- Plant card tap: opens Plant Details.

### Empty State

Title:

`Add your first plant`

Message:

`Create a care schedule with reminders and notes.`

Primary action:

`Add`

## Screen 4: Add Plant

### Purpose

Capture plant profile information and create custom care reminders.

### Layout

- Top app bar title: `Add Plant`
- Back button in top app bar.
- Scrollable content.
- Plant profile card.
- Care Reminders section.
- Reminder cards for each care type.
- Full-width primary `Save plant` button.

### Plant Profile Card

Fields:

- `Plant name`
- `Plant type`
- `Location`
- `Notes`

Photo area:

- Photo preview, or fallback plant icon.
- Button: `Choose photo`

Helper copy:

Title:

`Plant profile`

Body:

`Add the details that help you recognize and care for it.`

### Care Reminders

Section title:

`Care Reminders`

Each reminder row includes:

- Care glyph.
- Reminder label.
- Suggested frequency.
- Enable switch.
- If enabled:
  - `Every` numeric input with suffix `days`
  - `Starts in` numeric input with suffix `days`

Suggested frequency content:

- Watering: `Suggested every 4 days`
- Fertilizing: `Suggested every 30 days`
- Repotting: `Suggested every 180 days`
- Misting: `Suggested every 3 days`
- Pruning: `Suggested every 45 days`

Default enabled reminders:

- Watering
- Misting

Default disabled reminders:

- Fertilizing
- Repotting
- Pruning

### Buttons And Actions

- Back button: discard unsaved input and return to the previous screen.
- `Choose photo`: opens Android photo picker.
- Reminder switch: enables or disables that reminder.
- `Save plant`: creates the plant and opens Plant Details.

### Validation

- `Save plant` remains disabled until `Plant name` is not empty.
- If `Plant type` is empty, save as `Houseplant`.
- If `Location` is empty, save as `Unassigned`.
- Empty notes are allowed.
- Reminder frequency must be at least 1 day.
- Reminder start offset can be 0 or more days.

### Empty State

None. This screen is a form.

## Screen 5: Plant Details

### Purpose

Show one plant's profile, active reminders, health observations, and care history.

### Layout

- Top app bar:
  - Back button.
  - Plant name as title.
- Plant detail header card.
- `Reminders` section.
- Reminder rows.
- `Health Notes` section.
- Health note composer.
- Existing health note cards.
- `Care History` section.
- History rows.

### Header Card Layout

Includes:

- Plant photo or fallback plant icon.
- Plant name.
- Plant type.
- Location.
- Notes, if available.
- Summary chips:
  - `{count} reminders`
  - `{count} logs`

### Reminder Rows

Each reminder row includes:

- Care glyph.
- Reminder label.
- Schedule text:
  - `Every {frequencyDays} days - next {date}`
- `Done` button.

### Health Note Composer

Title:

`Log a quick observation`

Mood chips:

- `Needs attention`
- `Healthy`
- `New growth`

Input label:

`Yellow leaves, dry soil, new growth...`

Button:

`Add note`

### Buttons And Actions

- Back button: returns to the previous main screen.
- `Done`: logs care, adds a care history entry, and moves the next due date forward.
- Mood chip: sets note mood.
- `Add note`: saves the health note when text is not empty.

### Empty States

Reminders empty title:

`No reminders`

Reminders empty message:

`Add reminders when editing support is expanded.`

Care History empty title:

`No care logged`

Care History empty message:

`Tap Done on a reminder to build this plant's history.`

Health Notes empty state is handled by showing the composer first. No separate empty message is needed.

## Screen 6: Calendar

### Purpose

Show upcoming plant care tasks grouped by date.

### Layout

- Top app bar title: `Calendar`
- Summary card.
- Scrollable agenda grouped by due date.
- Date headers.
- Task rows under each date.
- Bottom navigation.

### Summary Card

Title:

`Upcoming care`

Subtitle when tasks exist:

`Your next {taskCount} scheduled tasks across {plantCount} plants.`

Subtitle when empty:

`Add plants and reminders to build your care calendar.`

Metric pills:

- `{plantCount} plants`
- `{reminderCount} reminders`

### Date Headers

Formats:

- Today: `Today, {MMM d}`
- Tomorrow: `Tomorrow, {MMM d}`
- Other dates: `{EEE, MMM d}`

Today uses a beige accent surface. Future dates use a light green surface.

### Calendar Task Row

Each row includes:

- Care glyph.
- Reminder label.
- Plant name and location:
  - `{plantName} - {location}`
- Open details chevron.

### Buttons And Actions

- Calendar task row tap: opens Plant Details.
- Bottom navigation: switches between main screens.

### Empty State

Title:

`Calendar is empty`

Message:

`New reminders will appear here automatically.`

## Screen 7: Settings

### Purpose

Show app reminder behavior, basic collection stats, and onboarding access.

### Layout

- Top app bar title: `Settings`
- Reminder settings/info card.
- Friendly defaults info card.
- Full-width outlined button to show onboarding again.
- Bottom navigation.

### Reminder Info Card

Title:

`Plant care reminders`

Body:

`WaterMe schedules local notifications around 9:00 AM.`

Stats rows:

- `Plants tracked`
- `Active reminders`
- `Care history entries`
- `Health notes`

### Friendly Defaults Card

Title:

`Friendly defaults`

Body:

`New plants start with watering and misting suggestions. You can enable fertilizing, repotting, and pruning while adding each plant.`

### Buttons And Actions

- `Show onboarding again`: opens the onboarding screen without deleting plant data.

### Empty State

None. Settings always shows app behavior and stats.

## Notifications

### Permission Prompt

On Android 13 and later, request notification permission after the app starts.

### Notification Content

Title format:

`{plantName} needs {careType}`

Body:

`Open WaterMe to log care and keep your plant on track.`

Channel name:

`Plant care reminders`

Channel description:

`Notifications for watering, fertilizing, repotting, misting, and pruning.`

### Notification Action

Tap notification: opens WaterMe to the main app.

## Global Empty State Rules

- Empty states use Warm Beige cards.
- Empty states should feel calm, not alarming.
- Empty state cards include a title and one short sentence.
- When a direct next action exists, put the action button near the empty state.

## Global User Actions

| Action | Entry Point | Result |
| --- | --- | --- |
| Complete onboarding | Onboarding | Opens Today |
| Add plant | Today, Plants | Opens Add Plant |
| Choose photo | Add Plant | Opens Android photo picker |
| Save plant | Add Plant | Creates plant and opens details |
| Open plant | Plant cards, task rows | Opens Plant Details |
| Complete care | Today, Plant Details | Adds care history and updates next due date |
| Add health note | Plant Details | Adds note to Health Notes |
| View upcoming care | Calendar tab | Opens Calendar |
| Show onboarding again | Settings | Opens Onboarding without deleting data |

## Mobile-First Android Behavior

- Use a single-column layout on phones.
- Keep bottom navigation reachable with one hand.
- Keep primary actions at the bottom or in reachable FAB positions.
- All lists are vertically scrollable.
- Cards span the available width.
- Text truncates gracefully for long plant names and locations.
- Touch targets must be at least 48 dp.
- Inputs use Android keyboard types, including numeric keyboard for reminder intervals.
- Back behavior follows Android expectations: nested screens return to the previous app screen, and main tabs remain at the bottom navigation level.
