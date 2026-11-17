# Graffiti Studio — Architecture & Technical Decisions

## 1. Modular Clean Architecture

```text
:app (MainActivity, AppContainer, Navigation)
 ├── :feature:home (HomeScreen, HomeViewModel, CreateProjectDialog)
 ├── :feature:workspace (WorkspaceScreen, WorkspaceViewModel)
 ├── :core:designsystem (Dark Urban theme, typography, reusable components)
 ├── :core:data (Room Database, DataStore preferences, ProjectRepositoryImpl)
 ├── :core:storage (Native .gsp ZIP+JSON format, atomic save, autosave recovery)
 ├── :engine:drawing (DrawingEngine, BrushRasterizer, StrokeStabilizer, DrawingSurfaceView)
 ├── :engine:rendering (RenderBackend, SoftwareCanvasRenderBackend, BitmapPool, TileGrid)
 └── :core:domain (Pure Kotlin JVM: models, HistoryManager, HistoryCommand, Repository interfaces)
```

### Dependency Boundaries
- **`:core:domain`** has zero Android or Compose dependencies and runs as a pure Kotlin/JVM module.
- **`:engine:drawing`** and **`:engine:rendering`** have zero Jetpack Compose dependencies. The UI hosts `DrawingSurfaceView` via `AndroidView`.

## 2. Command-Based History System

Undo and redo are implemented via `HistoryManager` and `HistoryCommand` in `:core:domain`:
- **Live Stroke Execution**: During `ACTION_MOVE`, `DrawingEngine` rasterizes quadratic Bezier segments directly onto the active layer's `Bitmap` for low-latency feedback.
- **Dirty-Rect Backup**: On `ACTION_UP`, `DrawingEngine` crops the pre-stroke layer pixels strictly to the stroke's bounding box (`dirtyRect`) using a pooled bitmap from `BitmapPool`, then calls `HistoryManager.recordExecuted(DrawStrokeCommand(...))`.
- **Reversibility**: `undo()` restores only `dirtyRect` pixels (`PorterDuff.Mode.SRC`) and removes the stroke from `LayerData`; `redo()` re-rasterizes the stroke and appends it back. Memory stays bounded (`maxHistorySize = 60`) without full-canvas snapshots.

## 3. Rendering & Performance Architecture

- **`RenderBackend` Seam**: `SoftwareCanvasRenderBackend` implements `RenderBackend` using Android `Canvas` and `PorterDuff` blend modes. A future `GlEs3RenderBackend` (using FBOs and tile textures) can be introduced behind `RenderBackend` without modifying `:engine:drawing` or `:feature:workspace`.
- **`BitmapPool`**: Pools mutable `ARGB_8888` bitmaps by `(width, height, config)` within a 32 MB budget and responds to `ComponentCallbacks2` memory trim levels.
- **`TileGrid`**: Tracks 256×256 dirty tiles so compositing only redraws regions touched by the active stroke or undo/redo command.

## 4. Native `.gsp` Project Format

Projects are stored as `.gsp` ZIP archives containing `manifest.json` (format version, document dimensions, background color, layers, and full vector `StrokeData` lists). Saves write to a `.tmp` file and atomically replace the target `.gsp` file.
