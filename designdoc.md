## Amnesia Plugin - Design Document

**Version:** 1.0
**Author:** ModPotato
**Date:** March 5, 2025

### 1. Overview

**Plugin Name:** Amnesia
**Description:** Amnesia is a highly configurable Minecraft plugin designed for PaperMC and Folia servers that shuffles crafting recipes. It offers two distinct shuffling modes: random item replacement and recipe result swapping. The plugin prioritizes performance and ease of use, allowing server administrators to create unpredictable and engaging gameplay experiences.  Amnesia also includes a timer-based reshuffle system with user notifications and seed-based rotations for repeatable and controlled recipe changes.

**Core Features:**

*   **Recipe Shuffling:** Two modes of recipe manipulation:
    *   **Random Item Mode:** Recipe outputs are replaced with random items from the Minecraft item registry.
    *   **Recipe Result Shuffle Mode:** Recipe outputs are shuffled amongst existing recipes.
*   **Timer-Based Reshuffling:**  Automatic reshuffling of recipes at configurable intervals with countdown notifications.
*   **Seed-Based Rotations:**  Ability to use seeds to generate and recall specific recipe shuffles, allowing for rotation management.
*   **Command-Line Interface:**  Comprehensive commands for managing shuffling, timer, seed, and configuration.
*   **Configuration File:** YAML based configuration for persistent settings and customization.
*   **Performance Focused:** Designed for minimal server impact, especially on Folia servers, utilizing asynchronous operations where possible.
*   **User Notifications:**  Clear and informative messages to players about upcoming and executed recipe shuffles using Adventure API for rich formatting.

**Target Audience:** Minecraft server administrators looking to add a unique and challenging gameplay element to their servers, particularly those using PaperMC or Folia.

### 2. Functional Requirements

This section details the specific features and functionalities of the Amnesia plugin.

**2.1. Recipe Shuffling Mechanics (2 Story Points)**

*   **FR.1.1. Two Shuffle Modes:**
    *   **FR.1.1.1. Random Item Mode:** When enabled, upon shuffling, each recipe's result will be replaced with a randomly selected item from the Minecraft server's item registry.
        *   The plugin should iterate through all registered recipes (excluding blacklisted recipes - see configuration).
        *   For each recipe, a random item from the item registry will be chosen as the new result.
        *   Consideration should be given to item rarity or configurable weighting of item chances if needed in future iterations.
    *   **FR.1.1.2. Recipe Result Shuffle Mode:** When enabled, upon shuffling, recipe results will be swapped among existing recipes.
        *   The plugin should collect all registered recipes (excluding blacklisted recipes).
        *   The results of these recipes will be shuffled amongst themselves. For example, the result of recipe A might become the result of recipe B, recipe B's result becomes recipe C's, and so on, with the last recipe's result becoming the first's, or a truly random permutation.

*   **FR.1.2. Recipe Handling:**
    *   **FR.1.2.1. Recipe Types:** The plugin should target all standard crafting recipes, including:
        *   Shaped Recipes
        *   Shapeless Recipes
        *   Smelting Recipes
        *   Blasting Recipes
        *   Smoking Recipes
        *   Campfire Recipes
        *   Stonecutting Recipes
        *   (Potentially future support for other recipe types like Smithing, if relevant and requested in future)
    *   **FR.1.2.2. Recipe Persistence:** Recipe changes should persist server restarts unless explicitly reverted or reshuffled again. The shuffled state should be the default state loaded on server startup after the first shuffle.

**2.2. Timer-Based Reshuffling (2 Story Points)**

*   **FR.2.1. Configurable Timer Interval:**
    *   The timer interval for automatic reshuffling should be configurable via the configuration file and a command.
    *   The interval should be specified in seconds or minutes (configurable unit in config).
*   **FR.2.2. Timer Enable/Disable:**
    *   The timer should be enabled or disabled via the configuration file and a command.
*   **FR.2.3. Countdown Notifications:**
    *   Before a timer-based reshuffle occurs, the plugin should send countdown notifications to all online players.
    *   Notification intervals should be configurable (e.g., 5 minutes, 1 minute, 30 seconds, 10, 9, 8... 1).
    *   Notification messages should be customizable via the configuration file and utilize Adventure API components for formatting.

**2.3. Seed-Based Rotation System (1 Story Point)**

*   **FR.3.1. Seed Generation and Usage:**
    *   The plugin should use a seed for the random number generation during shuffling.
    *   A default seed should be used if not specified.
    *   Administrators should be able to:
        *   View the current seed.
        *   Set a new seed via command.
        *   The seed should be saved in the configuration file for persistence across server restarts.
*   **FR.3.2. Seeded Shuffling:**
    *   When shuffling, the plugin should utilize the current seed to ensure deterministic recipe shuffling. Using the same seed will always produce the same shuffle order for a given plugin version and Minecraft version.
*   **FR.3.3. Seed Rotation (Future Consideration):** While not explicitly requested now, consider the potential for seed rotation history or saving seeds for "presets" for future development if user demand arises.

**2.4. Commands (1 Story Point)**

*   **FR.4.1. `/amnesia shuffle [mode]`:**
    *   Triggers an immediate recipe shuffle.
    *   `mode` argument (optional): Specifies the shuffle mode: `random_item` or `recipe_result`. If omitted, uses the default mode from config.
    *   Permission: `amnesia.command.shuffle`
*   **FR.4.2. `/amnesia timer [interval <seconds/minutes>] [enable/disable]`:**
    *   Manages the timer-based reshuffling.
    *   `interval <seconds/minutes>` (optional): Sets the timer interval. If omitted, shows the current interval. Needs to specify units.
    *   `enable/disable` (optional): Enables or disables the timer. If omitted, toggles the current state.
    *   Permission: `amnesia.command.timer`
*   **FR.4.3. `/amnesia seed [set <seed> | view]`:**
    *   Manages the shuffle seed.
    *   `set <seed>`: Sets a new seed.
    *   `view`: Displays the current seed.
    *   Permission: `amnesia.command.seed`
*   **FR.4.4. `/amnesia reload`:**
    *   Reloads the plugin configuration from the `config.yml` file.
    *   Permission: `amnesia.command.reload`

**2.5. Configuration (2 Story Points)**

*   **FR.5.1. `config.yml` File:**
    *   The plugin should use a `config.yml` file for all persistent settings.
    *   The configuration should be automatically loaded on plugin startup and reloaded via the `/amnesia reload` command.
*   **FR.5.2. Configuration Options:**
    *   `shuffle-mode`: (String, default: `random_item`) -  Default shuffle mode (`random_item` or `recipe_result`).
    *   `timer-interval`: (Integer, default: `3600` - seconds) - Timer interval in seconds for automatic reshuffling.
    *   `timer-enabled`: (Boolean, default: `false`) - Whether the timer-based reshuffling is enabled by default.
    *   `seed`: (Long, default: `<randomly generated on first run>`) - The seed used for recipe shuffling.
    *   `excluded-recipes`: (List of Strings, default: `[]`) - List of recipe keys (e.g., `minecraft:crafting_table`) to exclude from shuffling.
    *   `excluded-random-items`: (List of Strings, default: `[]`) - List of item keys (e.g., `minecraft:air`) to exclude from being selected in `random_item` shuffle mode. Useful for blacklisting potentially problematic items.
    *   `notification-intervals`: (List of Integers, default: `[300, 60, 30, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1]`) - Countdown intervals (in seconds) for timer notifications.
    *   `notification-messages`: (YAML structure for Adventure Components, default messages provided below) - Customizable notification messages for different countdown stages and shuffle start/end.

**Example `config.yml`:**

```yaml
shuffle-mode: "random_item" # or "recipe_result"
timer-interval: 3600 # seconds (1 hour)
timer-enabled: false
seed: 123456789
excluded-recipes:
  - "minecraft:stick" # Example: prevent stick recipe from being shuffled
excluded-random-items:
  - "minecraft:air" # Example: prevent air from being a random result
notification-intervals:
  - 300 # 5 minutes
  - 60  # 1 minute
  - 30  # 30 seconds
  - 10
  - 9
  - 8
  - 7
  - 6
  - 5
  - 4
  - 3
  - 2
  - 1
notification-messages:
  countdown-5-minutes: "<gold>5 minutes until recipes are shuffled!</gold>"
  countdown-1-minute: "<yellow>1 minute until recipes are shuffled!</yellow>"
  countdown-30-seconds: "<yellow>30 seconds until recipes are shuffled!</yellow>"
  countdown-10-seconds: "<red>Recipes will shuffle in <bold><seconds></bold> seconds!</red>" # <seconds> is a placeholder
  countdown-start: "<red><bold>Recipes will shuffle in...</bold></red>"
  shuffle-started: "<green><bold>Recipes have been shuffled!</bold></green>"
  shuffle-finished: "<green>Recipe shuffling complete.</green>"
```

### 3. Non-Functional Requirements

**3.1. Performance (Folia Support) (1 Story Point)**

*   **NFR.1.1. Asynchronous Operations:**  Recipe manipulation and timer tasks must be handled asynchronously to avoid blocking the main server thread, especially critical for Folia's region-ticked architecture. Use Folia's `RegionScheduler` or Paper's `BukkitScheduler` appropriately for asynchronous tasks.
*   **NFR.1.2. Efficient Algorithms:** Use efficient algorithms for recipe shuffling and data manipulation to minimize CPU usage.
*   **NFR.1.3. Memory Management:**  Ensure efficient memory usage to prevent memory leaks or excessive garbage collection.

**3.2. Configurability (Covered in Functional Requirements)**

*   The plugin must be highly configurable via the `config.yml` file and commands as detailed in section 2.5.

**3.3. User Experience (1 Story Point)**

*   **NFR.3.1. Clear Commands:** Commands should be intuitive and easy to use with tab completion for arguments.
*   **NFR.3.2. Informative Messages:**  Use Adventure API components to create clear, informative, and visually appealing messages for notifications and command feedback. Utilize colors, formatting, and potentially sound effects (if deemed beneficial in future iterations).
*   **NFR.3.3. Error Handling:**  Provide informative error messages when commands are used incorrectly or when configuration issues occur.

**3.4. Folia/PaperMC Support (Always Supported)**

*   **NFR.4.1. API Compatibility:**  The plugin must be compatible with both PaperMC and Folia APIs. Leverage common API elements and use Folia's specific features where performance gains are possible (like `RegionScheduler`).
*   **NFR.4.2. Asynchronous Nature:**  Strict adherence to asynchronous principles is crucial for Folia environments to maintain region isolation and prevent cross-region thread blocking.

### 4. Permissions

The following permission nodes are to be used:

*   `amnesia.command.shuffle`:  Allows use of `/amnesia shuffle` command. Default: OP
*   `amnesia.command.timer`: Allows use of `/amnesia timer` command. Default: OP
*   `amnesia.command.seed`:  Allows use of `/amnesia seed` command. Default: OP
*   `amnesia.command.reload`: Allows use of `/amnesia reload` command. Default: OP

Default permission levels are suggested as OP, but server owners can adjust these using permission plugins.

### 5. Technology Stack

*   **Programming Language:** Java
*   **Minecraft Server API:** PaperMC/Folia API
*   **Dependency Management:** Gradle Kotlin DSL (as per provided `build.gradle.kts`)
*   **Configuration Management:** YAML (SnakeYAML or similar library for Java)
*   **Text Formatting:** Adventure API (for rich text components)

### 6. Development Plan

**Story Point Breakdown:**

*   Configuration Handling: 2 SP
*   Command Framework: 1 SP
*   Recipe Shuffling Logic: 2 SP
*   Timer Implementation: 2 SP
*   Seed System: 1 SP
*   Notification System (Adventure API): 1 SP
*   Folia Support & Asynchronous Operations: 1 SP
*   Testing and Refinement: 2 SP

**Development Stages (Example - can be adjusted):**

1.  **Project Setup & Configuration Loading (2 SP):**
    *   Set up Gradle project.
    *   Implement `config.yml` loading and saving using YAML library.
    *   Create configuration class to hold settings.
2.  **Command Framework & Basic Commands (2 SP):**
    *   Implement a simple command handling framework.
    *   Implement `/amnesia reload` and `/amnesia seed view` commands.
    *   Implement basic tab completion.
3.  **Recipe Shuffling Logic (2 SP):**
    *   Implement both `random_item` and `recipe_result` shuffling modes.
    *   Implement recipe exclusion from configuration.
    *   Basic shuffling functionality without timer or seed yet.
4.  **Seed System Integration (1 SP):**
    *   Integrate seed generation and setting.
    *   Modify shuffling logic to use the seed for random number generation.
    *   Implement `/amnesia seed set` command.
5.  **Timer Implementation & Notifications (3 SP):**
    *   Implement timer functionality using Folia's `RegionScheduler` or Paper's `BukkitScheduler` for asynchronous tasks.
    *   Implement `/amnesia timer` command for interval setting and enabling/disabling.
    *   Implement basic countdown notifications to players using Adventure API components for formatting.
    *   Implement configurable notification messages from `config.yml`.
6.  **Refinement, Testing, and Bug Fixing (2 SP):**
    *   Thoroughly test all features and commands.
    *   Address any bugs or issues found.
    *   Improve user messages and feedback.
    *   Performance testing, especially on Folia if possible.

### 7. Future Considerations

*   **Recipe Blacklisting/Whitelisting:** More granular control over which recipes are shuffled.
*   **Item Category Filtering for Random Item Mode:**  Option to shuffle to random items within specific item categories (e.g., only tools, only blocks).
*   **Seed Rotation History/Presets:** Saving and recalling previous seeds for rotation management.
*   **GUI Configuration:**  In-game GUI for easier configuration management.
*   **API for other plugins:** Allow other plugins to interact with the Amnesia shuffling system.
*   **More advanced notification options:** Sound effects, action bar messages, title messages.
