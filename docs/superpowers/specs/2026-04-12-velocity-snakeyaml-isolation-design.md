# Velocity SnakeYAML Isolation Design

## Context

Floodgate's `core` module imports `org.yaml.snakeyaml` directly for database config loading. The `velocity` module currently declares SnakeYAML as a provided dependency because Velocity bundles Configurate, which also exposes SnakeYAML on the proxy classpath. That leaves Floodgate bound to whatever `org.yaml` version the runtime provides, which conflicts with other plugins that load different SnakeYAML versions.

## Goal

Make the Velocity plugin self-contained for SnakeYAML so it does not depend on a shared `org.yaml` package at runtime.

## Recommended Approach

Bundle SnakeYAML into the Velocity shadow jar and relocate `org.yaml` into Floodgate's shadow namespace, matching the existing isolation pattern already used by the Bungee and Spigot modules.

## Alternatives Considered

1. Keep SnakeYAML provided and rely on Velocity's bundled version.
   This preserves the current conflict surface and does not isolate Floodgate from other plugins.
2. Bundle SnakeYAML without relocation.
   This still leaves duplicate `org.yaml` classes on the runtime classpath and does not solve version collisions cleanly.
3. Remove direct SnakeYAML usage from `core`.
   This is broader than required for the packaging bug and would change working code paths unnecessarily.

## Design

Update `velocity/build.gradle.kts` so SnakeYAML is no longer marked as provided. Add `relocate("org.yaml")` so the shared shadow conventions move the packaged classes into `org.geysermc.floodgate.shadow.org.yaml`.

Verification should be artifact-focused:

1. Build the current Velocity shadow jar.
2. Inspect the jar contents and confirm the current packaging does not contain relocated SnakeYAML classes.
3. Apply the build change.
4. Rebuild and confirm the jar now contains relocated SnakeYAML classes and no top-level `org/yaml/...` entries.

## Testing

Use build-time verification by inspecting the resulting shadow jar. This is the smallest reliable test for a packaging issue and avoids introducing unrelated runtime changes.
