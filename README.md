# android-feed-reader

> Offline-first Android feed reader: Jetpack Compose + Room + Ktor client, single-source-of-truth sync. **A 50,000-article reconcile preserves every local read/bookmark flag with 0 losses, CI-gated under 250 ms.** Room owns the cache, the remote owns content, local owns user state. 13 JVM unit tests, builds a debug APK in CI.

[![ci](https://github.com/Tajaddin/android-feed-reader/actions/workflows/ci.yml/badge.svg)](https://github.com/Tajaddin/android-feed-reader/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0-7F52FF)](build.gradle.kts)
[![minSdk](https://img.shields.io/badge/minSdk-24-3DDC84)](app/build.gradle.kts)

## Hero metric

The hard part of offline-first is reconciling a remote feed with a local cache without losing the user's state. The pure `SyncReconciler` is unit-tested at scale:

| Property | Result |
|---|---:|
| Articles reconciled | 50,000 |
| Local read/bookmark flags lost | **0** |
| Bookmarked articles ever dropped | **0** |
| Reconcile time | **CI-gated < 250 ms** |

```bash
./gradlew testDebugUnitTest   # includes `preserves all flags across 50k reconcile under 250ms`
```

## The offline-first contract

A correct sync needs a clear source-of-truth split. `SyncReconciler.reconcile(local, remote)` enforces:

- **Remote owns content** — title, summary, url, publishedAt always come from the feed.
- **Local owns user state** — `isRead` and `isBookmarked` are preserved from the cache and never overwritten by a sync.
- **Tombstoning** — articles that drop out of the feed are evicted, **except bookmarked ones**, which are always kept.

A failed refresh (offline) leaves the cached feed fully intact and shows a banner, so the app is usable with no network.

The cache is read, reconciled and rewritten in one Room transaction (`ArticleDao.replaceWithReconciled`). A refresh that fails or is cancelled while writing rolls back instead of leaving the cache empty, and a read/bookmark toggle made during the write is applied after the commit instead of being overwritten.

## What it is

| Layer | Implementation |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation-Compose (list → detail) |
| State | `FeedViewModel` exposes `StateFlow`; the cache is observed as a `Flow` |
| Cache | **Room** (`ArticleDao` + `AppDatabase`), the single source of truth the UI reads |
| Network | **Ktor client** (OkHttp engine, kotlinx-serialization) behind a `FeedApi` interface |
| Sync | pure `SyncReconciler` + `ArticleRepository` (offline-first: read cache, refresh remote, reconcile, persist) |
| DI | manual (no framework), wired in `FeedApp` |

The repository depends on the `ArticleDao` and `FeedApi` **interfaces**, so the offline-first flow is tested with in-memory fakes, and the transactional refresh against a real in-memory Room database under Robolectric; no device or emulator.

## Why this matters for hiring

Role categories unlocked: **Android Engineer (Kotlin)**, Mobile.

This is the architecture senior Android interviews probe: Compose + Room + a networking client, a repository with a single source of truth, `StateFlow`, navigation, and a correct offline-first merge. The reconciler is the kind of pure, testable core that keeps an app correct under flaky networks.

## How to run

Prerequisites: JDK 17+ (the included Gradle wrapper handles the rest), Android SDK with platform-34 and build-tools 34. On CI, `android-actions/setup-android@v3` installs the SDK; locally, Android Studio's first launch installs them, or `sdkmanager "platforms;android-34" "build-tools;34.0.0"`.

```bash
./gradlew testDebugUnitTest   # 13 unit tests (reconciler, repository with fakes, Room refresh)
./gradlew assembleDebug       # builds app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk   # install on a connected device
```

Open in Android Studio and run on any device/emulator (API 24+). CI builds the APK and uploads it as an artifact.

## Architecture

```
ui/ (Compose + Navigation)
   FeedViewModel  ── StateFlow ──┐
                                 ▼
data/ArticleRepository   single source of truth
   ├── data/local/ArticleDao + AppDatabase   (Room cache, the truth the UI reads)
   ├── data/remote/FeedApi (Ktor) + RemoteArticle DTO
   └── sync/SyncReconciler   pure merge (the hero)
```

## Testing

```bash
./gradlew testDebugUnitTest   # 13 tests
```

- **SyncReconcilerTest** (7): add new, preserve flags on overlap, remote-content-wins, drop non-bookmarked, keep bookmarked, newest-first sort, and the **50k flag-preservation + perf** property.
- **ArticleRepositoryTest** (5): refresh populates cache, read flag survives re-sync, bookmarked survives feed drop, non-bookmarked evicted, toggle helpers — all on in-memory `ArticleDao` and `FeedApi` fakes.
- **RoomRefreshTest** (1): on a real in-memory Room database (Robolectric), a refresh that fails while rewriting the cache rolls back and keeps every cached article and bookmark.

The Ktor client is exercised by the production build only; its interface is faked in unit tests.

## Project layout

```
app/src/main/java/com/tajaddin/feedreader/
  data/Article.kt                 # domain model + Room entity
  data/ArticleRepository.kt       # offline-first single source of truth
  data/local/ArticleDao.kt        # Room DAO interface
  data/local/AppDatabase.kt       # Room database (KSP-generated impl)
  data/remote/FeedApi.kt          # Ktor client behind an interface
  data/remote/RemoteArticle.kt    # network DTO
  sync/SyncReconciler.kt          # pure merge (the hero)
  ui/                             # Compose screens, nav, ViewModel, FeedApp DI
```

## Stack

Kotlin 2.0, Jetpack Compose (BOM 2024.10), Material 3, Navigation-Compose, Room 2.6 (KSP), Ktor client 2.3 (OkHttp + kotlinx-serialization), AndroidX Lifecycle/StateFlow, JUnit4, kotlinx-coroutines-test, Robolectric, Gradle (wrapper), AGP 8.7, GitHub Actions. Targets JVM 17, minSdk 24.

## License

MIT
