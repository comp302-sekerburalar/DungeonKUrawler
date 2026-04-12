# Dungeon KUrawler
**COMP 302 – Software Engineering | Koç University – Spring 2026**

A pixel-art dungeon crawler built with Java 21 and JavaFX 21.

---

## Project Structure

```
DungeonKUrawler/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/kurawler/
    │       ├── app/
    │       │   └── Main.java              ← Entry point
    │       ├── screens/
    │       │   ├── ScreenManager.java     ← Navigation controller
    │       │   ├── BaseScreen.java        ← Abstract base
    │       │   ├── MainMenuScreen.java    ← Main menu with animated torches
    │       │   ├── LoginScreen.java       ← Login with tabbed toggle
    │       │   ├── RegisterScreen.java    ← Hero registration
    │       │   ├── WelcomeScreen.java     ← Post-login quest briefing
    │       │   └── HelpScreen.java        ← How-to-play reference
    │       ├── components/
    │       │   ├── DungeonButton.java     ← Styled pixel-art button
    │       │   ├── PixelTextField.java    ← Unified text / password input
    │       │   ├── TorchAnimation.java    ← Animated flickering torch
    │       │   └── PixelBorder.java       ← Stone-tile cap bars
    │       └── model/
    │           └── UserStore.java         ← SHA-256 hashed user accounts (JSON)
    └── resources/
        └── css/
            └── dungeon.css               ← Full dark dungeon theme
```

---

## Requirements

| Tool     | Version |
|----------|---------|
| Java JDK | 21+     |
| Maven    | 3.8+    |

JavaFX is pulled automatically by Maven from Maven Central - no manual download needed.

---

## Build & Run

### With Maven (recommended)

```bash
# Clone / unzip the project, then:
cd DungeonKUrawler

# Run directly
mvn javafx:run

# Build a fat JAR
mvn package
java -jar target/dungeon-kurawler-1.0-SNAPSHOT-shaded.jar
```

### With an IDE (IntelliJ IDEA / Eclipse)

1. Open the project as a **Maven** project.
2. Let the IDE import dependencies.
3. Run `com.kurawler.app.Main`.

If your IDE requires explicit VM args for JavaFX (older setups):
```
--module-path /path/to/javafx-sdk/lib
--add-modules javafx.controls,javafx.fxml,javafx.media
```

---

## User Accounts

Accounts are stored in `~/.kurawler/users.json` (your home directory).  
Passwords are hashed with **SHA-256** — never stored in plain text.

To test:
1. Launch the game → click **START NEW GAME**
2. Switch to the **REGISTER** tab
3. Create a hero name + password
4. Next launch → use **LOGIN** tab with the same credentials

---

## Keyboard Shortcuts

| Key       | Action                        |
|-----------|-------------------------------|
| `ENTER`   | Confirm / submit form         |
| `ESC`     | Back to previous screen       |
| `H`       | Open Help screen (main menu)  |
| `I`       | Open Inventory *(Phase 2)*    |
| `P`       | Pause / Resume *(Phase 2)*    |

---

## Phase 1 Scope

This deliverable covers:
- ✅ Main menu with animated torches & star background
- ✅ Login screen (SHA-256 hashed credentials)
- ✅ Registration screen with validation
- ✅ Welcome / quest briefing screen
- ✅ Help screen
- ✅ Persistent user store (`~/.kurawler/users.json`)
- ✅ Full CSS dungeon theme (dark stone, gold accents, red highlights)
- ✅ Keyboard navigation

Game engine, level editor and gameplay screens are Phase 2+.
