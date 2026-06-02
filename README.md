# Dungeon KUrawler

**COMP 302 – Software Engineering | Koç University – Spring 2026**

A pixel-art dungeon crawler built with Java 21 and JavaFX 21 featuring map creation, survival gameplay, combat, inventory management, marketplace progression, and persistent player accounts.

---

# Overview

Dungeon KUrawler is a dungeon exploration and survival game inspired by classic dungeon crawler RPGs. Players can design custom maps, fight enemies, collect loot, manage equipment, purchase upgrades, and survive increasingly difficult enemy waves.

The project was developed as part of the COMP 302 Software Engineering course and follows an object-oriented architecture emphasizing extensibility, maintainability, and reusable gameplay systems.

---

# Features

## Authentication & Persistence

* User registration and login system
* SHA-256 password hashing
* Persistent local user accounts
* Persistent player coin balances
* Persistent marketplace purchases
* Automatic save directory creation
* Session restoration support

User data is stored locally at:

```text
~/.kurawler/users.json
```

---

## Core Engine

* Tile-based grid engine
* Vec2 coordinate system
* Collision detection
* Map generation system
* Game loop architecture
* Combat system
* Projectile support
* Character statistics system
* Inventory management

---

## Character System

Hero statistics:

* HP (Health)
* Mana
* Strength (STR)
* Defense (DEF)
* Energy

Features:

* Equipment system
* Weapon equipping
* Inventory management
* Consumable items
* Stat modification system

---

## Combat System

Implemented mechanics include:

* Melee combat
* Enemy damage calculation
* Weapon damage modifiers
* Defense mitigation
* Hero death handling
* Enemy death handling
* Energy consumption during combat
* Click-to-attack interactions

Combat follows the specification requirements described in the project document.

---

## Enemy AI

### Knight

* Pathfinding toward player
* Melee attacks
* Dynamic stat scaling

### Sorcerer

* Projectile attacks
* Teleportation behavior
* Ranged combat

Additional features:

* Enemy spawning
* Wave scaling
* Elite enemies
* Dynamic difficulty progression

---

## Wave Survival Mode

An endless survival mode where:

* Enemies spawn continuously
* Wave difficulty increases over time
* Enemy counts scale every wave
* Spawn intervals become more aggressive
* Rewards increase as progression continues
* Game ends only when the hero dies

Wave mode acts similarly to endless-runner progression systems.

---

## Marketplace System

Players earn coins by defeating enemies and surviving waves.

Marketplace features:

* Coin economy
* Persistent purchases
* Item ownership tracking
* Purchase validation
* Dynamic marketplace UI

Available item categories:

* Weapons
* Potions
* Spell Scrolls
* Keys
* Rings

---

## Loadout System

The Loadout screen allows players to:

* View owned items
* Equip weapons
* Equip accessories
* Manage consumables
* Prepare loadouts before entering Wave Mode

Equipped items automatically synchronize with gameplay sessions.

---

## Map Editor

Features:

* Place dungeon objects
* Create custom maps
* Save maps
* Load maps
* Edit existing layouts
* Run maps directly from editor

Map data is stored using JSON serialization as required by the project specification.

---

## Visual Systems

* Pixel-art rendering
* Animated torches
* Sprite animation system
* Character animation
* Marketplace UI
* Inventory UI
* Loadout UI
* Responsive scaling

Utility systems include:

* SpriteRenderer
* ImageCache
* HeroAnimator
* KnightAnimator
* SorcererAnimator

---

# Project Structure

```text
DungeonKUrawler/
├── pom.xml
├── src/main/
│
├── java/com/kurawler/
│   ├── app/
│   ├── components/
│   ├── engine/
│   ├── game/
│   │   ├── action/
│   │   ├── effect/
│   │   ├── entity/
│   │   └── objects/
│   ├── model/
│   ├── screens/
│   ├── util/
│   └── wave/
│
└── resources/
    ├── css/
    └── images/
```

---

# Build Requirements

| Tool     | Version |
| -------- | ------- |
| Java JDK | 21+     |
| Maven    | 3.8+    |

---

# Build & Run

## Maven

```bash
git clone <repository-url>

cd DungeonKUrawler

mvn javafx:run
```

## Build JAR

```bash
mvn package

java -jar target/dungeon-kurawler.jar
```

---

# Controls

| Key              | Action            |
| ---------------- | ----------------- |
| W A S D / Arrows | Move Hero         |
| Mouse Click      | Interact / Attack |
| I                | Inventory         |
| ESC              | Pause Menu        |
| ENTER            | Confirm           |
| H                | Help Screen       |

---

# Technologies

* Java 21
* JavaFX 21
* Maven
* JSON Serialization
* SHA-256 Authentication
* Object-Oriented Design Patterns

---

# Current Development Status

## Phase 1

* Authentication System
* Main Menu
* Help Screen
* Welcome Screen
* User Persistence
* Dungeon UI Theme

## Phase 2

* Game Engine
* Combat System
* Enemy AI
* Wave Survival Mode
* Marketplace
* Loadout System
* Map Editor
* Save/Load Infrastructure (in progress)
* Team Match Mode (planned)
* Shadow Clone Mechanic (planned)

---

# Team

COMP 302 – Software Engineering

Koç University – Spring 2026
