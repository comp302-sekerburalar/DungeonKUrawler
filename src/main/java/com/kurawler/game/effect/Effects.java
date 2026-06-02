package com.kurawler.game.effect;

import com.kurawler.engine.GameEngine;
import com.kurawler.game.entity.StatType;
import com.kurawler.game.objects.GameObject;

// ─────────────────────────────────────────────
//  AddToInventoryEffect
// ─────────────────────────────────────────────
class AddToInventoryEffectImpl implements Effect {
    @Override
    public void apply(GameEngine engine, GameObject subject) {
        if (engine.getHero().getInventory().isFull()) {
            engine.postMessage("Inventory full! Cannot pick up " + subject.getName() + ".");
            return;
        }
        engine.getMap().removeObject(subject);
        engine.getHero().getInventory().add(subject);
        engine.postMessage("Picked up: " + subject.getName());
        engine.notifyMapChanged();
        engine.notifyInventoryChanged();
    }
}

// ─────────────────────────────────────────────
// ModifyStatEffect (EAT potion, etc.)
// ─────────────────────────────────────────────
class ModifyStatEffectImpl implements Effect {
    private final StatType stat;
    private final int delta;
    private final boolean consume;

    ModifyStatEffectImpl(StatType stat, int delta, boolean consume) {
        this.stat = stat;
        this.delta = delta;
        this.consume = consume;
    }

    @Override
    public void apply(GameEngine engine, GameObject subject) {
        int before = engine.getHero().getStat(stat);
        engine.getHero().modifyStat(stat, delta);
        int after = engine.getHero().getStat(stat);
        String sign = delta >= 0 ? "+" : "";
        engine.postMessage(stat.displayName() + " " + sign + delta +
                "  (" + before + " → " + after + ")");
        if (consume) {
            engine.getHero().getInventory().remove(subject);
            engine.postMessage("Used: " + subject.getName());
            engine.notifyInventoryChanged();
        }
        engine.notifyStatsChanged();
    }
}

// ─────────────────────────────────────────────
// EquipWeaponEffect
// ─────────────────────────────────────────────
class EquipWeaponEffectImpl implements Effect {
    @Override
    public void apply(GameEngine engine, GameObject subject) {
        engine.getHero().equipWeapon(subject);
        engine.postMessage("Equipped: " + subject.getName());
        engine.notifyInventoryChanged();
    }
}

// ─────────────────────────────────────────────
// WearArmorEffect — adds DEF
// ─────────────────────────────────────────────
class WearArmorEffectImpl implements Effect {
    private final int defBonus;

    WearArmorEffectImpl(int defBonus) {
        this.defBonus = defBonus;
    }

    @Override
    public void apply(GameEngine engine, GameObject subject) {
        engine.getHero().modifyStat(StatType.DEF, defBonus);
        engine.getHero().getInventory().remove(subject);
        engine.postMessage("Wore " + subject.getName() + " (+DEF " + defBonus + ")");
        engine.notifyStatsChanged();
        engine.notifyInventoryChanged();
    }
}

// ─────────────────────────────────────────────
// SearchEffect — hidden item discovery
// ─────────────────────────────────────────────
class SearchEffectImpl implements Effect {
    private final GameObject hiddenItem; // null = nothing hidden

    SearchEffectImpl(GameObject hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    @Override
    public void apply(GameEngine engine, GameObject subject) {
        if (hiddenItem == null) {
            engine.postMessage("You search the " + subject.getName() + "... nothing found.");
            return;
        }
        if (engine.getHero().getInventory().isFull()) {
            engine.postMessage("Found " + hiddenItem.getName() + " but inventory is full!");
            return;
        }
        engine.getHero().getInventory().add(hiddenItem);
        engine.postMessage("You found a " + hiddenItem.getName() + "!");
        engine.notifyInventoryChanged();
        // Remove this action so it can't be searched again
        subject.clearActions();
        subject.addAction(new com.kurawler.game.action.Action(
                "Search " + subject.getName(),
                new SearchEffectImpl(null)));
    }
}

// ─────────────────────────────────────────────
// BreakEffect — STR-based probability
// ─────────────────────────────────────────────
class BreakEffectImpl implements Effect {
    @Override
    public void apply(GameEngine engine, GameObject subject) {
        int str = engine.getHero().getStat(StatType.STR);
        // Break probability: clamp STR 8-15 → prob 0.40 – 0.95
        double prob = 0.30 + (str / 15.0) * 0.65;
        engine.getHero().spendEnergy(com.kurawler.game.entity.Hero.ENERGY_COST_BREAK);
        engine.notifyStatsChanged();
        if (Math.random() < prob) {
            engine.getMap().removeObject(subject);
            engine.postMessage("Broke " + subject.getName() + "!");
            engine.notifyMapChanged();
        } else {
            engine.postMessage("Failed to break " + subject.getName() + ". (STR too low?)");
        }
    }
}

class ShadowCloneEffectImpl implements Effect {
    @Override
    public void apply(GameEngine engine, GameObject subject) {
        if (!engine.getHero().getInventory().all().contains(subject)) {
            engine.postMessage("Pick up the scroll before reading it.");
            return;
        }
        if (engine.activateShadowClone()) {
            engine.getHero().getInventory().remove(subject);
            engine.postMessage("Read Shadow Clone scroll.");
            engine.notifyInventoryChanged();
        }
    }
}

// ─────────────────────────────────────────────
// Public factory (avoids exposing *Impl names)
// ─────────────────────────────────────────────
public final class Effects {
    private Effects() {
    }

    public static Effect addToInventory() {
        return new AddToInventoryEffectImpl();
    }

    public static Effect modifyStat(StatType t, int d, boolean c) {
        return new ModifyStatEffectImpl(t, d, c);
    }

    public static Effect equipWeapon() {
        return new EquipWeaponEffectImpl();
    }

    public static Effect wearArmor(int defBonus) {
        return new WearArmorEffectImpl(defBonus);
    }

    public static Effect search(GameObject hidden) {
        return new SearchEffectImpl(hidden);
    }

    public static Effect breakObject() {
        return new BreakEffectImpl();
    }

    public static Effect shadowClone() {
        return new ShadowCloneEffectImpl();
    }
}
