package com.ridelink.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountStatusTest {

    @Test
    @DisplayName("Should contain ACTIVE and INACTIVE account statuses")
    void testAccountStatusEnumValues() {
        AccountStatus[] expectedStatuses = {AccountStatus.ACTIVE, AccountStatus.INACTIVE};
        assertArrayEquals(expectedStatuses, AccountStatus.values());
    }

    @Test
    @DisplayName("Should correctly parse account status from string")
    void testAccountStatusValueOf() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.INACTIVE, AccountStatus.valueOf("INACTIVE"));
    }
}
