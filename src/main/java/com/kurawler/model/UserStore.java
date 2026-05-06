package com.kurawler.model;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.HexFormat;

/**
 * Persistent user account store backed by a simple JSON file.
 *
 * Passwords are stored as SHA-256 hashes (hex strings).
 * The save file is written to the user's home directory:
 *   ~/.kurawler/users.json
 *
 * File format:
 * {
 *   "HERONAME": "sha256hexhash",
 *   ...
 * }
 */
public class UserStore {

    private static final Path SAVE_DIR  = Path.of(System.getProperty("user.home"), ".kurawler");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("users.json");

    /** username (uppercase) -> SHA-256 hex hash of password */
    private final Map<String, String> users = new HashMap<>();

    public UserStore() {
        load();
    }

    // ---------- Public API ----------

    /**
     * Authenticate a user.
     * @return true if the username exists and the password matches
     */
    public boolean authenticate(String username, String password) {
        String key  = username.trim().toUpperCase();
        String hash = hash(password);
        return hash.equals(users.get(key));
    }

    /**
     * Register a new user. Caller must ensure username is not already taken.
     */
    public void register(String username, String password) {
        String key = username.trim().toUpperCase();
        users.put(key, hash(password));
        save();
    }

    /**
     * Check whether a username is already registered (case-insensitive).
     */
    public boolean exists(String username) {
        return users.containsKey(username.trim().toUpperCase());
    }

    // ---------- Persistence ----------

    private void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            String content = Files.readString(SAVE_FILE);
            parseJson(content);
        } catch (IOException e) {
            System.err.println("[UserStore] Could not load users: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            Files.writeString(SAVE_FILE, toJson(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[UserStore] Could not save users: " + e.getMessage());
        }
    }

    // ---------- Minimal JSON helpers (no external deps) ----------

    private String toJson() {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, String> entry : users.entrySet()) {
            sb.append("  \"")
              .append(escape(entry.getKey()))
              .append("\": \"")
              .append(escape(entry.getValue()))
              .append("\"");
            if (i < users.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private void parseJson(String json) {
        // Very simple line-by-line parser for our own known format
        for (String line : json.split("\n")) {
            line = line.trim();
            if (line.startsWith("\"") && line.contains(":")) {
                String[] parts = line.split("\":\\s*\"", 2);
                if (parts.length == 2) {
                    String key = parts[0].replace("\"", "").trim();
                    String val = parts[1].replaceAll("\",?$", "").trim();
                    if (!key.isEmpty() && !val.isEmpty()) {
                        users.put(key, val);
                    }
                }
            }
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---------- Hashing ----------

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in the JDK; fall back to plain if somehow missing
            System.err.println("[UserStore] SHA-256 not available, storing plain text as fallback.");
            return password;
        }
    }
}
