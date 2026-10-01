package com.ridelink.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    @DisplayName("Should contain PASSENGER, DRIVER, and ADMIN roles")
    void testRoleEnumValues() {
        Role[] expectedRoles = {Role.PASSENGER, Role.DRIVER, Role.ADMIN};
        assertArrayEquals(expectedRoles, Role.values());
    }

    @Test
    @DisplayName("Should correctly parse role from string")
    void testRoleValueOf() {
        assertEquals(Role.PASSENGER, Role.valueOf("PASSENGER"));
        assertEquals(Role.DRIVER, Role.valueOf("DRIVER"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
    }
}
