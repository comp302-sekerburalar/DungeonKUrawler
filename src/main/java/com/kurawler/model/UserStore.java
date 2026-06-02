package com.kurawler.model;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Persists accounts, coins, owned items, equipment, and consumable quantities.
 *
 * Line format:
 * "USER": "hash:coins:weapon:armor:skin:item1,item2:con_id=qty,..."
 */
public class UserStore {

    private static final Path SAVE_DIR = Path.of(System.getProperty("user.home"), ".kurawler");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("users.json");

    private final Map<String, String> passwords = new HashMap<>();
    private final Map<String, Integer> coins = new HashMap<>();
    private final Map<String, Set<String>> ownedItems = new HashMap<>();
    private final Map<String, String> eqWeapon = new HashMap<>();
    private final Map<String, String> eqArmor = new HashMap<>();
    private final Map<String, String> eqSkin = new HashMap<>();
    /** username -> (consumableId -> quantity) */
    private final Map<String, Map<String, Integer>> consumables = new HashMap<>();

    public UserStore() {
        System.out.println("SAVE FILE = " + SAVE_FILE.toAbsolutePath());
        load();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    public boolean authenticate(String u, String password) {
        return hash(password).equals(passwords.get(key(u)));
    }

    public void register(String u, String password) {
        String k = key(u);
        passwords.put(k, hash(password));
        coins.put(k, 0);
        Set<String> items = new HashSet<>();
        items.add("w_dagger");
        ownedItems.put(k, items);
        eqWeapon.put(k, "w_dagger");
        consumables.put(k, new HashMap<>());
        save();
    }

    public boolean exists(String u) {
        return passwords.containsKey(key(u));
    }

    // ── Coins ────────────────────────────────────────────────────────────────
    public int getCoins(String u) {
        return coins.getOrDefault(key(u), 0);
    }

    public void addCoins(String u, int n) {
        coins.merge(key(u), n, Integer::sum);
        save();
    }

    public boolean spendCoins(String u, int n) {
        int have = getCoins(u);
        if (have < n)
            return false;
        coins.put(key(u), have - n);
        save();
        return true;
    }

    // ── Permanent ownership ───────────────────────────────────────────────────
    public boolean ownsItem(String u, String id) {
        return ownedItems.getOrDefault(key(u), Collections.emptySet()).contains(id);
    }

    public Set<String> getOwnedItems(String u) {
        return new HashSet<>(ownedItems.getOrDefault(key(u), Collections.emptySet()));
    }

    public void unlockItem(String u, String id) {
        ownedItems.computeIfAbsent(key(u), k -> new HashSet<>()).add(id);
        save();
    }

    public boolean isUnlocked(String u, String id) {
        return ownsItem(u, id);
    }

    // ── Equipment ─────────────────────────────────────────────────────────────
    public void equipWeapon(String u, String id) {
        eqWeapon.put(key(u), id);
        save();
    }

    public String getEquippedWeapon(String u) {
        return eqWeapon.get(key(u));
    }

    public void equipArmor(String u, String id) {
        eqArmor.put(key(u), id);
        save();
    }

    public String getEquippedArmor(String u) {
        return eqArmor.get(key(u));
    }

    public void equipSkin(String u, String id) {
        eqSkin.put(key(u), id);
        save();
    }

    public String getEquippedSkin(String u) {
        return eqSkin.get(key(u));
    }

    // ── Consumable quantities ─────────────────────────────────────────────────
    public int getConsumableQty(String u, String id) {
        return consumables.getOrDefault(key(u), Collections.emptyMap()).getOrDefault(id, 0);
    }

    public void addConsumable(String u, String id, int n) {
        consumables.computeIfAbsent(key(u), k -> new HashMap<>()).merge(id, n, Integer::sum);
        save();
    }

    public boolean useConsumable(String u, String id) {
        Map<String, Integer> m = consumables.get(key(u));
        if (m == null)
            return false;
        int qty = m.getOrDefault(id, 0);
        if (qty <= 0)
            return false;
        if (qty == 1)
            m.remove(id);
        else
            m.put(id, qty - 1);
        save();
        return true;
    }

    /** All consumables with qty > 0. */
    public Map<String, Integer> getAllConsumables(String u) {
        Map<String, Integer> raw = consumables.getOrDefault(key(u), Collections.emptyMap());
        Map<String, Integer> result = new LinkedHashMap<>();
        raw.forEach((id, qty) -> {
            if (qty > 0)
                result.put(id, qty);
        });
        return result;
    }

    // ── Unified purchase ──────────────────────────────────────────────────────
    /**
     * Buy an item:
     * isConsumable=true → increment quantity (allows repurchase)
     * isConsumable=false → permanent unlock (blocks duplicate)
     */
    public boolean buyItem(String u, String id, int price, boolean isConsumable) {
        if (isConsumable) {
            if (!spendCoins(u, price))
                return false;
            addConsumable(u, id, 1);
            return true;
        } else {
            if (ownsItem(u, id))
                return false;
            if (!spendCoins(u, price))
                return false;
            unlockItem(u, id);
            return true;
        }
    }

    /** Legacy — treats as permanent. */
    public boolean buyItem(String u, String id, int price) {
        return buyItem(u, id, price, false);
    }

    // ── Persistence ──────────────────────────────────────────────────────────
    private void load() {
        if (!Files.exists(SAVE_FILE))
            return;
        try {
            for (String line : Files.readAllLines(SAVE_FILE)) {
                line = line.trim();
                if (!line.startsWith("\""))
                    continue;
                int q2 = line.indexOf("\"", 1);
                if (q2 < 0)
                    continue;
                String user = line.substring(1, q2);
                int vs = line.indexOf("\"", q2 + 2), ve = line.indexOf("\"", vs + 1);
                if (vs < 0 || ve < 0)
                    continue;
                String val = line.substring(vs + 1, ve);
                String[] p = val.split(":", -1);

                passwords.put(user, p[0]);
                coins.put(user, p.length > 1 ? parseIntSafe(p[1]) : 0);
                if (p.length > 2 && !p[2].isBlank())
                    eqWeapon.put(user, p[2]);
                if (p.length > 3 && !p[3].isBlank())
                    eqArmor.put(user, p[3]);
                if (p.length > 4 && !p[4].isBlank())
                    eqSkin.put(user, p[4]);
                if (p.length > 5 && !p[5].isBlank()) {
                    ownedItems.put(user, new HashSet<>(Arrays.asList(p[5].split(","))));
                }
                if (p.length > 6 && !p[6].isBlank()) {
                    Map<String, Integer> cons = new HashMap<>();
                    for (String entry : p[6].split(",")) {
                        String[] kv = entry.split("=", 2);
                        if (kv.length == 2)
                            cons.put(kv[0], parseIntSafe(kv[1]));
                    }
                    consumables.put(user, cons);
                }
            }
        } catch (IOException e) {
            System.err.println("[UserStore] Load: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (String user : passwords.keySet()) {
                String hash = passwords.get(user);
                int coin = coins.getOrDefault(user, 0);
                String weapon = eqWeapon.getOrDefault(user, "");
                String armor = eqArmor.getOrDefault(user, "");
                String skin = eqSkin.getOrDefault(user, "");
                String items = String.join(",", ownedItems.getOrDefault(user, Collections.emptySet()));
                Map<String, Integer> cons = consumables.getOrDefault(user, Collections.emptyMap());
                StringBuilder cStr = new StringBuilder();
                cons.forEach((id, qty) -> {
                    if (qty > 0) {
                        if (cStr.length() > 0)
                            cStr.append(",");
                        cStr.append(id).append("=").append(qty);
                    }
                });
                sb.append("  \"").append(esc(user)).append("\": \"")
                        .append(esc(hash)).append(":").append(coin).append(":")
                        .append(weapon).append(":").append(armor).append(":").append(skin).append(":")
                        .append(items).append(":").append(cStr).append("\"");
                if (i++ < passwords.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append("}");
            Files.writeString(SAVE_FILE, sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[UserStore] Save: " + e.getMessage());
        }
    }

    private String key(String u) {
        return (u == null || u.isBlank()) ? "GUEST" : u.trim().toUpperCase();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String hash(String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(pw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : d)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return pw;
        }
    }
}
/ /   c o i n   e c o n o m y   u p d a t e 
 
 / /   b a l a n c e   s y n c   u p d a t e 
 
 