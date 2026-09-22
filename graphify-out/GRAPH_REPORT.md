# Graph Report - Dockie  (2026-09-23)

## Corpus Check
- 38 files · ~17,416 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 349 nodes · 416 edges · 41 communities (33 shown, 8 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 8 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `11442833`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- AppState
- DockieRepository
- .onCreate
- PowerSource
- DockMonitoringService
- .awaitDisableLocked
- ScreenTimeoutController
- NotificationController
- PermissionManager
- BootReceiver
- gradlew
- What You Must Do When Invoked
- Dockie
- graphify reference: extra exports and benchmark
- graphify reference: query, path, explain
- graphify reference: add a URL and watch a folder
- graphify reference: commit hook and native CLAUDE.md integration
- graphify reference: incremental update and cluster-only
- graphify reference: GitHub clone and cross-repo merge
- graphify reference: transcribe video and audio
- AGENTS.md
- extraction-spec.md
- AwakeFormat
- Play Console submission answers (Dockie)
- Dockie signing migration (Play App Signing)
- Testers — Dockie closed testing
- Decisions — Dockie
- Foreground service demo video — Dockie
- Closed test plan — Dockie
- Handoff — Dockie
- Gemini image requests — Dockie Play graphics
- ACTIVE_WORK.md
- PROJECT_STATE.md
- WORKFLOW.md

## God Nodes (most connected - your core abstractions)
1. `DockieRepository` - 30 edges
2. `DockMonitoringService` - 21 edges
3. `MainViewModel` - 21 edges
4. `Dockie` - 17 edges
5. `Play Console submission answers (Dockie)` - 16 edges
6. `What You Must Do When Invoked` - 12 edges
7. `AppState` - 11 edges
8. `/graphify` - 10 edges
9. `NotificationController` - 9 edges
10. `SettingsScreen()` - 8 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --references--> `MainViewModel`  [EXTRACTED]
  app/src/main/java/com/dockie/app/MainActivity.kt → app/src/main/java/com/dockie/app/ui/MainViewModel.kt
- `DockMonitoringService` --references--> `DockieRepository`  [EXTRACTED]
  app/src/main/java/com/dockie/app/service/DockMonitoringService.kt → app/src/main/java/com/dockie/app/data/DockieRepository.kt
- `MainViewModel` --references--> `AppState`  [EXTRACTED]
  app/src/main/java/com/dockie/app/ui/MainViewModel.kt → app/src/main/java/com/dockie/app/model/AppState.kt
- `MainScreen()` --calls--> `DockieControl()`  [INFERRED]
  app/src/main/java/com/dockie/app/ui/screens/MainScreen.kt → app/src/main/java/com/dockie/app/ui/components/DockieControl.kt
- `MainScreen()` --calls--> `StatusCard()`  [INFERRED]
  app/src/main/java/com/dockie/app/ui/screens/MainScreen.kt → app/src/main/java/com/dockie/app/ui/components/StatusCard.kt

## Import Cycles
- None detected.

## Communities (41 total, 8 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.10
Nodes (15): AndroidViewModel, AdvancedInfo, DockieFlags, formatTimeout(), Job, MainViewModel, PowerInfo, CustomDurationDialog() (+7 more)

### Community 1 - "AppState"
Cohesion: 0.12
Nodes (17): AppState, Disabled, Docked, Error, Monitoring, Onboarding, PermissionRequired, Bulb (+9 more)

### Community 2 - "DockieRepository"
Cohesion: 0.07
Nodes (5): DockieRepository, Keys, DockieApp, Application, Flow

### Community 3 - ".onCreate"
Cohesion: 0.15
Nodes (11): MainActivity, DockArt(), DotsRow(), Modifier, OnboardingFlow(), RestoreArt(), Modifier, PermissionScreen() (+3 more)

### Community 4 - "PowerSource"
Cohesion: 0.22
Nodes (9): ChargingStateObserver, Context, PowerSnapshot, PowerSource, NONE, UNKNOWN, WIRED_AC, WIRED_USB (+1 more)

### Community 5 - "DockMonitoringService"
Cohesion: 0.22
Nodes (6): DisableActionReceiver, DockMonitoringService, BroadcastReceiver, Job, IntentFilter, Service

### Community 6 - ".awaitDisableLocked"
Cohesion: 0.27
Nodes (7): DockController, Context, Intent, resetState(), start(), stop(), IBinder

### Community 7 - "ScreenTimeoutController"
Cohesion: 0.39
Nodes (3): Context, ScreenTimeoutController, ContentResolver

### Community 8 - "NotificationController"
Cohesion: 0.38
Nodes (4): Context, NotificationController, Notification, PendingIntent

### Community 9 - "PermissionManager"
Cohesion: 0.40
Nodes (3): Context, Intent, PermissionManager

### Community 10 - "BootReceiver"
Cohesion: 0.33
Nodes (4): BootReceiver, BroadcastReceiver, Context, Intent

### Community 11 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 17 - "What You Must Do When Invoked"
Cohesion: 0.08
Nodes (24): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+16 more)

### Community 18 - "Dockie"
Cohesion: 0.11
Nodes (17): 100% battery behavior, After a manual screen-off, Background-service design, Build instructions, Dockie, Docking alert and status-bar icon, Download, Google Play (Closed Testing) (+9 more)

### Community 19 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 20 - "graphify reference: query, path, explain"
Cohesion: 0.33
Nodes (5): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal

### Community 21 - "graphify reference: add a URL and watch a folder"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 22 - "graphify reference: commit hook and native CLAUDE.md integration"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

### Community 23 - "graphify reference: incremental update and cluster-only"
Cohesion: 0.50
Nodes (3): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only

### Community 29 - "Play Console submission answers (Dockie)"
Cohesion: 0.09
Nodes (21): Ads, App access, Content rating (IARC questionnaire — expected answers), COVID-19 contact tracing / status apps, Data safety, Data types, Financial features, Foreground service declaration (+13 more)

### Community 30 - "Dockie signing migration (Play App Signing)"
Cohesion: 0.22
Nodes (8): Artifact certificate match (verified), Dockie signing migration (Play App Signing), Exact safe enrollment procedure, Existing production identity (verified), First AAB to upload, ONE remaining manual Play action, Play certificate status, Prepared local files (never commit)

### Community 31 - "Testers — Dockie closed testing"
Cohesion: 0.25
Nodes (7): Exact opt-in steps (send to each tester), How to give useful feedback, Message you can send to testers, Tester list, Testers — Dockie closed testing, What to test (short), Why this matters

### Community 32 - "Decisions — Dockie"
Cohesion: 0.29
Nodes (6): D1 — Wireless-only trigger, D2 — Timeout ownership model, D3 — Foreground service type, D4 — Signing continuity, D5 — Privacy, Decisions — Dockie

### Community 33 - "Foreground service demo video — Dockie"
Cohesion: 0.29
Nodes (6): Caption / narration (ready to read), Foreground service demo video — Dockie, Manual remaining, Recording steps (device or emulator), Target, What the reviewer must see

### Community 34 - "Closed test plan — Dockie"
Cohesion: 0.33
Nodes (5): Closed test plan — Dockie, Feedback format for testers, Pass criteria for closed-test start, Preconditions, Test matrix

### Community 35 - "Handoff — Dockie"
Cohesion: 0.50
Nodes (3): Exact next steps, Handoff — Dockie, Verified current state (2026-09-22)

### Community 36 - "Gemini image requests — Dockie Play graphics"
Cohesion: 0.50
Nodes (3): 1) Feature graphic, 2) Optional polished 512×512 store icon (only if upgrading), Gemini image requests — Dockie Play graphics

## Knowledge Gaps
- **123 isolated node(s):** `Keys`, `Onboarding`, `PermissionRequired`, `Disabled`, `Monitoring` (+118 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DockMonitoringService` connect `DockMonitoringService` to `DockieRepository`, `.awaitDisableLocked`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._
- **Why does `DockieRepository` connect `DockieRepository` to `DockMonitoringService`, `.awaitDisableLocked`?**
  _High betweenness centrality (0.031) - this node is a cross-community bridge._
- **Why does `MainViewModel` connect `MainViewModel` to `AppState`, `.onCreate`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `Keys`, `Onboarding`, `PermissionRequired` to the rest of the system?**
  _123 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.0989247311827957 - nodes in this community are weakly interconnected._
- **Should `AppState` be split into smaller, more focused modules?**
  _Cohesion score 0.12380952380952381 - nodes in this community are weakly interconnected._
- **Should `DockieRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._