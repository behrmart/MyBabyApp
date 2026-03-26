# AGENTS.md

## Project state
This repository is the Android client for Video Locker.
It currently starts from a small Jetpack Compose Android app in a single `app` module.
The existing package is `com.example.mybabyapp`; keep it unless explicitly told to rename it later.

## Product goal
Implement a native Android client for the existing Video Locker backend.

Target feature set, in this order:
1. App scaffold and navigation
2. Authentication and session handling
3. Videos and GIFs
4. Photos and albums
5. External server media browser
6. Admin-only features

Do not try to build the entire product in one pass.

## Architecture
- Kotlin only
- Jetpack Compose only
- Keep a single `app` module for now
- Use a clear UI layer + data layer
- Use state holders for UI state
- Prefer unidirectional data flow
- Keep packages organized by responsibility

Recommended package structure:
- `data/api`
- `data/auth`
- `data/model`
- `data/repository`
- `ui/navigation`
- `ui/auth`
- `ui/videos`
- `ui/gifs`
- `ui/photos`
- `ui/servermedia`
- `ui/admin`
- `ui/components`
- `util`

Do not introduce a domain layer, dependency injection framework, or multi-module Gradle setup unless explicitly requested.

## Required libraries and patterns
- Navigation Compose for screen navigation
- Retrofit for API interfaces
- OkHttp for HTTP client and auth header injection
- A single JSON serialization approach, preferably kotlinx serialization unless another serializer is already required
- Coil for images
- Media3 / ExoPlayer for video playback
- DataStore for simple persisted app/session state
- Keep auth/session storage behind a small abstraction

Do not add libraries outside this set unless clearly justified.

## Backend contract
Use the provided Video Locker specification as the source of truth for endpoints and behavior.

Important rules:
- Protected endpoints use `Authorization: Bearer <token>`
- For Android, prefer Bearer headers; do not use the query-token workaround unless explicitly needed
- Treat the `server-media` item id as an opaque string
- A 401 means session invalid/expired: clear session and return to login
- There is no refresh-token flow
- There is no logout endpoint
- Admin APIs must only be surfaced in the UI for ADMIN users
- Videos and GIFs share `/api/videos`; separate them by `mimeType`
- Do not invent backend fields or endpoints

## Networking and environments
- Do not hardcode a single base URL directly in UI code
- Use a clean configuration strategy for dev vs prod
- If cleartext HTTP is needed for local/LAN development, restrict it to development configuration only and justify any manifest or network-security changes before making them
- Prefer HTTPS for production
- Never log tokens, passwords, or raw Authorization headers

## Workflow
For any non-trivial task:
1. Read the current project and this file first
2. Plan first before editing
3. State exactly which files will change
4. Keep the diff minimal
5. Make one vertical slice at a time
6. Validate before moving on
7. Preserve working behavior unless the task explicitly replaces it

Never:
- rewrite the whole app in one pass
- rename packages broadly without approval
- introduce navigation, storage, playback, and admin features all at once
- edit `AndroidManifest.xml` without explaining why first
- add permissions without explaining why first

## Slice order
Implement in this order only:
1. Scaffold + navigation shell
2. Login + session handling
3. Video list + GIF list + detail + comments + view increment
4. Photo albums + photo grid + full-screen viewer
5. External server media browser + playback/viewer
6. Admin dashboard features

Do not jump ahead.

## Auth rules
- Implement login first
- No registration screen in the first slice unless explicitly requested
- Persist session carefully
- On 401 from protected APIs, clear session and navigate to login
- Keep role available to the UI so admin sections can be hidden for non-admin users
- Do not implement refresh tokens

## Media rules
- Use Media3 for standard video playback
- Use authenticated requests for protected streams
- Use Coil for authenticated or fetched image content where appropriate
- Keep media handling simple and reliable; avoid clever custom pipelines unless necessary

## Validation
After edits:
- Run the smallest relevant validation first
- Then run `./gradlew :app:compileDebugKotlin`
- Then run `./gradlew :app:assembleDebug`
- If the slice is runnable, test it on emulator or device
- Summarize any manual checks still needed

## Coding style
- Prefer readable, boring code over clever code
- Small composables
- Small repositories
- Small API interfaces
- Avoid giant files when a slice naturally justifies splitting them
- Keep names explicit
- Keep comments sparse and useful

## Out of scope until later
- Full offline cache strategy
- Pagination beyond what is needed for immediate usability
- Push notifications
- Multi-module refactor
- Dependency injection framework
- Registration flow
- Tablet-specific optimizations
- Search, favorites, or download features

## UI polish rules
- Use Material 3 styling consistently
- Prefer a dark-first visual design for this app
- Keep navigation and data flow unchanged unless required for fullscreen media behavior
- Fullscreen / immersive mode is allowed only for video detail and photo viewer screens
- Do not hide system bars on normal list or admin screens
- Support portrait and landscape layouts for media screens
- Do not hard-lock the whole app orientation
- Keep UI changes incremental and reviewable