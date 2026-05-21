package com.kurawler.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserStoreTest {

    @TempDir
    static Path tempHomeDirectory;

    private UserStore store;

    @BeforeAll
    public static void setupSandboxEnvironment() {
        // Redirects user.home system property into a temporary folder 
        // BEFORE the UserStore class loads its static properties.
        System.setProperty("user.home", tempHomeDirectory.toAbsolutePath().toString());
    }

    @BeforeEach
    public void init() throws IOException {
        // Clean target sandbox path to ensure tests start fresh
        Path saveFile = tempHomeDirectory.resolve(".kurawler").resolve("users.json");
        Files.deleteIfExists(saveFile);
        store = new UserStore();
    }

    // ---------- Helper Wrapper for repOk Assertion ----------
    
    private void assertRepresentationInvariant(UserStore instance) {
        try {
            // Extracts internal map via reflection to compute invariant check
            Field usersField = UserStore.class.getDeclaredField("users");
            usersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) usersField.get(instance);

            assertNotNull(users, "RI Violation: Internal database map cannot be null");
            for (Map.Entry<String, String> entry : users.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();

                assertNotNull(key, "RI Violation: Username key cannot be null");
                assertFalse(key.isEmpty(), "RI Violation: Username key cannot be blank");
                assertEquals(key.trim().toUpperCase(), key, "RI Violation: Keys must be trimmed and upper-case");
                assertNotNull(val, "RI Violation: Hash value cannot be null");
                assertEquals(64, val.length(), "RI Violation: Hash value must be exactly 64-hex characters");
                assertTrue(val.matches("^[0-9a-fA-F]+$"), "RI Violation: Hash must match hexadecimal structure");
            }
        } catch (Exception e) {
            fail("Failed to verify structural Rep Invariant due to: " + e.getMessage());
        }
    }

    // ---------- Targeted Tests for authenticate() ----------

    @Test
    public void testAuthenticateValidUser() {
        store.register("AlphaHero", "SecurePass99!");
        assertTrue(store.authenticate("AlphaHero", "SecurePass99!"));
        assertRepresentationInvariant(store);
    }

    @Test
    public void testAuthenticateInvalidPassword() {
        store.register("BetaHero", "CorrectPassword");
        assertFalse(store.authenticate("BetaHero", "WrongPassword"));
        assertRepresentationInvariant(store);
    }

    @Test
    public void testAuthenticateCaseAndWhitespaceInsensitivity() {
        store.register("GammaHero", "PasswordABC");
        // Check mixed casing and extraneous white spaces
        assertTrue(store.authenticate("   gAmMaHeRo   ", "PasswordABC"));
        assertRepresentationInvariant(store);
    }

    @Test
    public void testAuthenticateNonExistentUser() {
        assertFalse(store.authenticate("GhostUser", "anyPassword"));
        assertRepresentationInvariant(store);
    }

    // ---------- ADT Structure & Lifecycle Tests ----------

    @Test
    public void testInitialStateEmptyOrLoaded() {
        // Verifies baseline initialization is clean and doesn't break the invariant
        assertFalse(store.exists("AnyUser"));
        assertRepresentationInvariant(store);
    }

    @Test
    public void testRegisterValidUserChangesState() {
        assertFalse(store.exists("NewUser"));
        
        store.register("NewUser", "MyPass123");
        
        assertTrue(store.exists("NewUser"));
        assertTrue(store.exists("newuser")); // Checking case insensitivity via exists()
        assertRepresentationInvariant(store);
    }

    @Test
    public void testRegisterDuplicateUserOverwrites() {
        store.register("DuplicateUser", "FirstPassword");
        store.register("duplicateuser", "SecondPassword"); // Same identifier, matching capitalization path

        assertTrue(store.authenticate("DUPLICATEUSER", "SecondPassword"));
        assertFalse(store.authenticate("DUPLICATEUSER", "FirstPassword"));
        assertRepresentationInvariant(store);
    }

    @Test
    public void testPersistenceLoadAndSave() {
        store.register("PersistentHero", "KeepMeSafe");
        assertRepresentationInvariant(store);

        // Instantiating a secondary store to verify system read/write synchronization
        UserStore secondaryStore = new UserStore();
        assertTrue(secondaryStore.exists("PersistentHero"));
        assertTrue(secondaryStore.authenticate("PersistentHero", "KeepMeSafe"));
        assertRepresentationInvariant(secondaryStore);
    }
}