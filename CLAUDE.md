# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build / Run

```bash
./gradlew build       # compile and package the mod JAR
./gradlew runClient   # launch Minecraft client with the mod loaded
```

Gradle 9.6.1, Fabric Loom plugin, Java 21. The mod targets Minecraft 1.21.11 with Fabric API ≥0.141.4.

## Architecture

Safeguard is a **client-side Fabric mod** that detects in-game dangers and issues warnings or automatic protection
actions. Everything runs locally — no server-side component.

### Detection–Action Pattern

The core of the mod is a **Detection**– **Action** pattern with a hierarchical toggle tree:

- **`Detection`** (abstract, `protection.detection`) — detects a specific danger (creepers, lava, fall, low health,
  etc.). Each detection owns one or more **`Action`** instances (1:N binding). A detection can selectively
  enable/disable each bound action independently (`actionBindings` in config).
- **`Action`** (abstract, `protection.action`) — a response triggered by a detection (ActionBarTitle, PlaySound, Pause,
  Quit, Outline, RedVignette, BlockOutline, Auto MLG).

Both detections and actions live in a **`SwitchTreeNode`** tree — a hierarchical toggle where disabling a parent (e.g.
"Combat") disables all children (e.g. "Anti Creeper"). This is the configurable on/off system presented in the YACL GUI.

Beyond on/off, both `Detection` and `Action` can declare **`ConfigOption`** fields (package `protection.option`:
`IntOption`/`DoubleOption`/`BoolOption`/`ColorOption`) via `registerOption(...)`, replacing what used to be hardcoded
`@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})` placeholder fields. An option is its own single source of
truth (`.get()`/`.set(T)`, with clamping/validation), rendered in the YACL screen next to its owner's toggle. Action
options are **per action ID, global** by default (an action class can be `new`'d by several detections but shares one
config, fanned out via `ProtectionManager.getActionInstances(id)`); an option can opt into
`.pairScoped()` to become **detection–action-pair specific** instead — used when a detection-local static subclass of
an `Action` (e.g. `LavaDetection.LavaBlockOutlineAction extends BlockOutlineAction`) needs its own value (typically a
presentation parameter like highlight color) without polluting the shared action config. Pair-scoped options persist
under the detection leaf's `actionOptions` and render in the "Links" category next to their binding checkbox, keeping
"detections only detect, actions only decide how to respond" — see `OutlineAction`/`BlockOutlineAction` for the
reference shape (abstract base + protected color-aware method + detection-local subclass).

### Initialization Flow

`Safeguard.onInitializeClient()` creates the four singletons and wires them through `ModContext` (a `record` acting as a
DI container):

1. `ProtectionManager` — registers all detections + actions, builds toggle trees
2. `ConfigManager` — loads/saves `safeguard.json` via Gson with backup-on-corruption
3. `FilledThroughWallsRenderer` — custom `RenderPipeline` for x-ray block highlighting
4. Then: config loaded, `ConfigScreen` + `SafeguardCommand` initialized, Iris compat registered

### Event System: GatedEvent

`GatedEvent<T>` wraps a Fabric `Event<T>` with **per-owner suspend/resume**. When a detection is toggled off,
`Detection.applyActiveState(false)` suspends all its event listeners — no per-tick checks in hot paths. The gate factory
in `ClientPlayerTickEvents.GATED_START_TICK` also runs `GlobalProtectionConditions.shouldProtect()` before dispatching,
skipping all detections when the player is in creative/spectator, invulnerable, or has Resistance 255.

### Key Classes

| Class                        | Role                                                                                                      |
|------------------------------|-----------------------------------------------------------------------------------------------------------|
| `Safeguard`                  | `ClientModInitializer` entry point                                                                        |
| `ProtectionManager`          | Central registry: detection→actions map, detection tree, action tree                                      |
| `SwitchTreeNode`             | Hierarchical toggle with effective-state propagation (parent disable → all children effectively disabled) |
| `Detection` (abstract)       | Base for all danger detectors; manages bound actions, gated event lifecycle and its own `ConfigOption`s   |
| `Action` (abstract)          | Base for all protection responses; also manages its own `ConfigOption`s                                   |
| `ConfigOption<T>` (abstract) | Single-source-of-truth typed config value (`protection.option`); subclasses: `IntOption`/`DoubleOption`/`BoolOption`/`ColorOption` |
| `GatedEvent<T>`              | Fabric Event wrapper with suspend/resume per owner                                                        |
| `GlobalProtectionConditions` | Runtime predicates (creative, spectator, invulnerable, resistance) — AND-gate before any detection runs   |
| `ConfigManager`              | JSON persistence with backup-on-corruption                                                                |
| `ConfigScreen`               | YACL GUI: recursively builds toggle + `ConfigOption` widgets from `SwitchTreeNode` trees                  |
| `SafeguardCommand`           | `/safeguard screen/detection/action` with tab completion                                                  |
| `FilledThroughWallsRenderer` | Custom `RenderPipeline` (`FILLED_THROUGH_WALLS`) for drawing translucent boxes through terrain            |
| `IrisCompat`                 | Reflection-based Iris shader compatibility                                                                |
| `ModContext`                 | Record holding the four core singleton references                                                         |

### Mixins

- `ClientPlayerEntityMixin` — injects into `ClientPlayerEntity.tick()` to fire `ClientPlayerTickEvents.START_TICK`
- `LivingEntityMixin` — injects into `LivingEntity.onDamaged()` to fire `EntityDamagedEvents.PRE`
- `GameRendererMixin` — injects into `GameRenderer.close()` to fire `GameRendererCloseEvent` (cleanup for
  `FilledThroughWallsRenderer`)
- `EntityRendererMixin` / `InGameHudMixin` / `KeyBindingAccessor` — entity outlining, HUD rendering, key simulation

### Adding a New Detection

1. Subclass `Detection`, passing the path (e.g. `"combat/xyz"`) and bound `Action` instances to `super()`
2. Call `listen(GATED_EVENT, this::onEvent)` in the constructor to subscribe to a gated event
3. In the event handler, call `tryExecuteAction(ActionClass.class, action -> action.doThing(...))` to trigger actions
4. Register in `ProtectionManager.init()` via `register(new AntiXyzDetection())`
5. Add translation keys in `src/main/resources/assets/safeguard/lang/`
6. For any hardcoded threshold/range/color, declare a `ConfigOption` field (e.g.
   `private final IntOption distance = registerOption(IntOption.of("distance", 12).range(2, 32));`) instead of a plain
   field, and read it via `.get()`. Add a matching `detection.safeguard.<path>.option.<key>` translation key.

### Adding a New Action

1. Subclass `Action`, passing an ID path to `super()`
2. Override `defaultEnabled()` if the default should be `false`
3. The action is invoked by its parent `Detection` via `tryExecuteAction()`
4. Same as detections, configurable parameters should be `ConfigOption` fields registered via `registerOption(...)`.
   If the parameter is a presentation detail that should vary per detection (e.g. a highlight color), make the action
   abstract with a `protected` method, and have each detection declare a local static subclass holding a
   `.pairScoped()` option — see `OutlineAction`/`AntiAmbushDetection.AmbushOutlineAction` for the reference shape.

### Config Format

`ConfigManager` stores toggle state and config options as a JSON tree in `.minecraft/config/safeguard.json`. Detection
leaf nodes include `options` (the detection's own `ConfigOption`s), `actionBindings` (per-detection action toggles) and
`actionOptions` (pair-scoped options of bound actions, keyed by full action ID). Action leaf nodes include `options`
for global (non-pair-scoped) option values, fanned out to every action instance sharing that ID on load. See the
comment block at the bottom of `ConfigManager.java` for the full expected structure.
