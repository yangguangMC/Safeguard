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
| `Detection` (abstract)       | Base for all danger detectors; manages bound actions and gated event lifecycle                            |
| `Action` (abstract)          | Base for all protection responses                                                                         |
| `GatedEvent<T>`              | Fabric Event wrapper with suspend/resume per owner                                                        |
| `GlobalProtectionConditions` | Runtime predicates (creative, spectator, invulnerable, resistance) — AND-gate before any detection runs   |
| `ConfigManager`              | JSON persistence with backup-on-corruption                                                                |
| `ConfigScreen`               | YACL GUI: recursively builds toggle options from `SwitchTreeNode` trees                                   |
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

### Adding a New Action

1. Subclass `Action`, passing an ID path to `super()`
2. Override `defaultEnabled()` if the default should be `false`
3. The action is invoked by its parent `Detection` via `tryExecuteAction()`

### Config Format

`ConfigManager` stores toggle state as a JSON tree in `.minecraft/config/safeguard.json`. Detection leaf nodes include
`actionBindings` (per-detection action toggles). See the comment block at the bottom of `ConfigManager.java` for the
full expected structure.
