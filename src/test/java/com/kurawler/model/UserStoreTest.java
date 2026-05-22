package com.kurawler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreTest {

    @Test
    void registeredUserCanLoginWithCorrectPassword() {
        UserStore store = new UserStore();

        store.register("HuseynTestUser", "12345");

        assertTrue(store.authenticate("HuseynTestUser", "12345"));
    }

    @Test
    void registeredUserCannotLoginWithWrongPassword() {
        UserStore store = new UserStore();

        store.register("HuseynWrongPasswordUser", "12345");

        assertFalse(store.authenticate("HuseynWrongPasswordUser", "wrong"));
    }

    @Test
    void usernameLoginShouldBeCaseInsensitive() {
        UserStore store = new UserStore();

        store.register("HuseynCaseUser", "12345");

        assertTrue(store.authenticate("huseyncaseuser", "12345"));
    }
}
