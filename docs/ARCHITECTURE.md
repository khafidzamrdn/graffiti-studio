# Graffiti Studio — Architecture & Technical Decisions

## 1. Modular Clean Architecture

```text
:app (MainActivity, AppContainer, Navigation)
 ├── :feature:home (HomeScreen, HomeViewModel, CreateProjectDialog, RenameProjectDialog)
 ├── :feature:workspace (WorkspaceScreen, WorkspaceViewModel, BrushStudioPanel, LayerPanel, TypographyDialog, EffectsStudioDialog, ColorPickerDialog, ExportDialog)
 ├── :core:designsystem (Dark Urban theme, typography, reusable components)
 ├── :core:data (Room Database, DataStore preferences, ProjectRepositoryImpl, DataFactory)
 ├── :core:storage (Native .gsp ZIP+JSON format, atomic save, autosave recovery, ArtworkExporter)
 ├── :engine:drawing (DrawingEngine, BrushRasterizer, GraffitiTextRenderer, LayerEffectsRenderer, StrokeStabilizer, DrawingSurfaceView)
 ├── :engine:rendering (RenderBackend, SoftwareCanvasRenderBackend, BitmapPool, TileGrid)
 └── :core:domain (Pure Kotlin JVM: models, HistoryManager, HistoryCommand, Repository interfaces)
```

### Dependency Boundaries
- **`:core:domain`** has zero Android or Compose dependencies and runs as a pure Kotlin/JVM module.
- **`:engine:drawing`** and **`:engine:rendering`** have zero Jetpack Compose dependencies. The UI hosts `DrawingSurfaceView` via `AndroidView`.
- **`:app`** accesses data dependencies through `DataFactory` and `ProjectRepository` without directly depending on Room internals.

## 2. Command-Based History System

Undo and redo are implemented via `HistoryManager` and `HistoryCommand` in `:core:domain`:
- **Strokes (`DrawStrokeCommand`)**: During `ACTION_MOVE`, `DrawingEngine` rasterizes live segments onto the active layer's `Bitmap`. On `ACTION_UP`, it crops the pre-stroke backup strictly to the stroke's `dirtyRect` using `BitmapPool` and records `DrawStrokeCommand` via `HistoryManager.recordExecuted`.
- **Layers, Typography & Effects (`LayerStructureCommand`, `UpdateLayerMetadataCommand`)**: Structural edits (add/delete/reorder/duplicate/merge layers, graffiti text, layer effects) and metadata edits (visibility, opacity, blend mode, rename) are reversible and never re-rasterize unchanged layers.

## 3. Brush, Typography & Effects Engines

- **Brush Engine (`BrushRasterizer`)**: Supports `Pencil`, `Pen`, `Marker`, `Airbrush`, `Spray Paint`, `Texture Brush`, and `Eraser` with exponential moving-average stabilization (`StrokeStabilizer`) and deterministic `seed`-based particle generation.
- **Graffiti Typography (`GraffitiTextRenderer`)**: Renders editable `GraffitiTextElement`s with `Bubble`, `Chrome`, `3D Block`, and `Standard` styles, outline, drop shadow, stepped 3D extrusion, and gradient fills.
- **Effects Studio (`LayerEffectsRenderer`)**: Applies deterministic `Spray Texture`, `Paint Splatter`, `Glow`, `Blur`, `Noise`, and `Halftone` per layer.

## 4. Rendering, Storage & Export

- **`RenderBackend` Seam**: `SoftwareCanvasRenderBackend` composites layers with `PorterDuff` blend modes, `BitmapPool` (32 MB budget + `ComponentCallbacks2` trim), and 256×256 `TileGrid` dirty tracking, ready for future `GlEs3RenderBackend` integration.
- **Native `.gsp` & Autosave (`GspProjectStorage`)**: Atomic ZIP+JSON saves preserving layers, strokes, text elements, and effects, plus 20-second background autosave recovery.
- **Export (`ArtworkExporter`)**: Off-thread scaled compositing to PNG (with optional transparent background) and JPEG via `MediaStore` (`Pictures/GraffitiStudio/`).
