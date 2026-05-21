package com.kurawler.screens;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.kurawler.model.UserStore;

public class LoginScreenTest {

    // --- The Magic Fix: Tricks JavaFX into running without a real window ---
    @BeforeAll
    public static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit is already running, safely ignore
        }
    }

    private LoginScreen createIsolatedScreen() {
        // Pass null to completely skip building real managers
        LoginScreen screen = new LoginScreen(null);
        
        // Build a private sandboxed account store matching your logic specifications
        UserStore mockStore = new UserStore();
        mockStore.register("VALID_HERO", "SECRET123");
        
        screen.setTestFallbackStore(mockStore);
        return screen;
    }

    // Test 1: Empty input boundary check
    @Test
    public void testLoginFailsWithEmptyInputs() {
        LoginScreen screen = createIsolatedScreen();
        
        screen.processLoginLogic("", "SECRET123");
        assertFalse(screen.isTestWelcomeRedirectTriggered(), "Should not route user onward when fields are blank");
    }

    // Test 2: Invalid credentials boundary check
    @Test
    public void testLoginFailsWithWrongCredentials() {
        LoginScreen screen = createIsolatedScreen();

        screen.processLoginLogic("VALID_HERO", "WRONG_PASSWORD");
        assertFalse(screen.isTestWelcomeRedirectTriggered(), "Should not route user onward when password fails validation");
    }

    // Test 3: Successful execution path check
    @Test
    public void testLoginSucceedsWithValidCredentials() {
        LoginScreen screen = createIsolatedScreen();

        screen.processLoginLogic("VALID_HERO", "SECRET123");
        assertTrue(screen.isTestWelcomeRedirectTriggered(), "Should successfully flag route tracking variables as matching specifications");
    }
}