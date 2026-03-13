# AGENTS.md

## Project
Android app built with Kotlin and Jetpack Compose.
Single-activity app.
This project is an MVP photo gallery app.

## Current goal
Transform the default starter app into a simple gallery MVP:
- one main screen
- one button to select multiple photos
- display selected photos in a grid

## Rules
- Use Kotlin only
- Use Jetpack Compose only
- Keep a single-activity structure
- Keep the implementation simple and readable
- Do not introduce new dependencies unless clearly necessary
- Do not change Gradle files unless required
- Do not rename packages or restructure the whole project unless requested
- Prefer small, focused changes over large rewrites

## Workflow
- For non-trivial tasks, plan first before editing files
- Explain which files will change before making broad changes
- After changes, verify the project still builds
- Keep diffs minimal
- Preserve existing working behavior unless the task requires changing it

## Build / validation
After making changes, run the smallest relevant validation first.
For significant changes, ensure the app still builds.

## UI guidance
- Prefer simple Material 3 Compose UI
- Keep the first version minimal
- No bottom navigation, drawer, or extra screens unless requested

## Out of scope for now
- Full device media library scanning
- Albums
- Delete/share/edit features
- Cloud sync
- Authentication
