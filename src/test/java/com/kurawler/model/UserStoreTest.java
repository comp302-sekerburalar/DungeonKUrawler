package com.kurawler.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserStoreTest {

    private UserStore store;

    @BeforeAll
    public static void configureSandboxEnvironment() {
        // Redirects user.home to the system temp directory BEFORE UserStore 
        // class loads its static file path configurations.
        System.setProperty("user.home", System.getProperty("java.io.tmpdir"));
    }

    @BeforeEach
    public void init() throws IOException {
        // Deletes any existing test profiles to guarantee isolated clean states
        Path testFile = Path.of(System.getProperty("java.io.tmpdir"), ".kurawler", "users.json");
        Files.deleteIfExists(testFile);
        
        store = new UserStore();
    }

    @Test
    public void testInitializationState() {
        assertFalse(store.exists("Admin"));
        
        // Verifies the initial safe state passes representation invariant rules
        assertDoesNotThrow(() -> store.repOk());
    }

    @Test
    public void testRegisterUserEnforcement() {
        store.register("AlphaHero", "SecurePass99!");
        
        assertTrue(store.exists("AlphaHero"));
        assertTrue(store.exists("alphahero")); // Confirms case-insensitivity checks
        
        // Verifies the store is structurally sound after mutator modifications
        assertDoesNotThrow(() -> store.repOk());
    }

    @Test
    public void testDuplicateRegistrationOverwrites() {
        store.register("BetaHero", "FirstPassword");
        store.register("betahero", "SecondPassword"); // Matches identity normalizing paths
        
        assertTrue(store.authenticate("BETAHERO", "SecondPassword"));
        assertFalse(store.authenticate("BETAHERO", "FirstPassword"));
        
        assertDoesNotThrow(() -> store.repOk());
    }

    @Test
    public void testPersistenceLoadAndSave() {
        store.register("SavedHero", "CryptoKey2026");
        assertDoesNotThrow(() -> store.repOk());

        // Instantiate a second store instance to confirm file-load processing routines match
        UserStore physicalReloadStore = new UserStore();
        assertTrue(physicalReloadStore.exists("SavedHero"));
        assertTrue(physicalReloadStore.authenticate("SavedHero", "CryptoKey2026"));
        
        assertDoesNotThrow(() -> physicalReloadStore.repOk());
    }

    /** 
     * Tests for authenticate method and its 3 test cases:
     * 1. Valid credentials matching exactly should return true.
     * 2. Incorrect password attempts on valid user entries should return false.
     * 3. Variations in spacing and case inputs must normalize perfectly and return true.
     * 
     */
    @Test
    public void testAuthenticateValidCredentials() {
        store.register("GammaHero", "LetMeIn2026");

        assertTrue(store.authenticate("GammaHero", "LetMeIn2026"));
    }

    @Test
    public void testAuthenticateInvalidPassword() {
        store.register("GammaHero", "LetMeIn2026");

        assertFalse(store.authenticate("GammaHero", "WrongPasswordAttempt"));
    }

    @Test
    public void testAuthenticateCaseAndWhitespaceNormalization() {
        store.register("GammaHero", "LetMeIn2026");

        // Validates parsing logic handles formatting variance gracefully
        assertTrue(store.authenticate("   gAmMaHeRo   ", "LetMeIn2026"));
    }
}