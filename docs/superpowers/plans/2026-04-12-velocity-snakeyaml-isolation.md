# Velocity SnakeYAML Isolation Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Velocity plugin ship with an isolated SnakeYAML copy so shared runtime versions cannot break Floodgate.

**Architecture:** Keep the fix inside the existing Gradle shadow packaging path. The `velocity` module should package SnakeYAML as an implementation dependency and relocate `org.yaml` into Floodgate's shadow namespace, with verification based on the built artifact contents.

**Tech Stack:** Gradle Kotlin DSL, Shadow plugin, Java plugin packaging

---

## Chunk 1: Documentation And Baseline

### Task 1: Record the approved design

**Files:**
- Create: `docs/superpowers/specs/2026-04-12-velocity-snakeyaml-isolation-design.md`
- Create: `docs/superpowers/plans/2026-04-12-velocity-snakeyaml-isolation.md`

- [ ] **Step 1: Add the spec file**
- [ ] **Step 2: Add the implementation plan file**
- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-04-12-velocity-snakeyaml-isolation-design.md docs/superpowers/plans/2026-04-12-velocity-snakeyaml-isolation.md
git commit -m "docs: add velocity snakeyaml isolation plan"
```

## Chunk 2: Packaging Verification

### Task 2: Capture the failing packaging state

**Files:**
- Verify: `velocity/build.gradle.kts`

- [ ] **Step 1: Build the Velocity shadow jar**

```bash
.\gradlew.bat :velocity:shadowJar
```

- [ ] **Step 2: Inspect the jar and confirm relocated SnakeYAML is absent**

```bash
jar tf velocity/build/libs/floodgate-velocity.jar | findstr /I "snakeyaml org/geysermc/floodgate/shadow/org/yaml org/yaml/"
```

Expected before the fix: no relocated `org/geysermc/floodgate/shadow/org/yaml/...` entries.

## Chunk 3: Build Fix

### Task 3: Shade and relocate SnakeYAML in Velocity

**Files:**
- Modify: `velocity/build.gradle.kts`

- [ ] **Step 1: Write the minimal build change**
  Remove the `provided("org.yaml", "snakeyaml", ...)` declaration.
  Add `relocate("org.yaml")` alongside the other Velocity relocations.
  Add `implementation("org.yaml", "snakeyaml", Versions.snakeyamlVersion)` in dependencies.

- [ ] **Step 2: Rebuild the Velocity shadow jar**

```bash
.\gradlew.bat :velocity:shadowJar
```

- [ ] **Step 3: Inspect the jar contents**

```bash
jar tf velocity/build/libs/floodgate-velocity.jar | findstr /I "org/geysermc/floodgate/shadow/org/yaml org/yaml/"
```

Expected after the fix: relocated `org/geysermc/floodgate/shadow/org/yaml/...` entries exist, while top-level `org/yaml/...` entries do not.

- [ ] **Step 4: Commit**

```bash
git add velocity/build.gradle.kts
git commit -m "fix: isolate snakeyaml in velocity"
```

## Chunk 4: Final Verification

### Task 4: Final checks and operator guidance

**Files:**
- Verify: `velocity/build.gradle.kts`
- Verify: `velocity/build/libs/floodgate-velocity.jar`

- [ ] **Step 1: Run the final shadow jar build again if needed**
- [ ] **Step 2: Summarize the packaging result**
- [ ] **Step 3: Provide copy-pasteable build/use instructions**
