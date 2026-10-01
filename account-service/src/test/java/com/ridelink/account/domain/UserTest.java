package com.ridelink.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("Should default status to ACTIVE on no-arg constructor")
    void testDefaultConstructor() {
        User user = new User();
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertNull(user.getId());
        assertNull(user.getEmail());
    }

    @Test
    @DisplayName("Should correctly initialize all fields via full constructor")
    void testFullConstructor() {
        User user = new User(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "$2a$12$e8Y4JgKz7Q3bY9v1...",
                "+94771234567",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );

        assertEquals("665f1a2b3c4d5e6f7a8b9c0d", user.getId());
        assertEquals("Kavindu", user.getFirstName());
        assertEquals("Umayanga", user.getLastName());
        assertEquals("Kavindu Umayanga", user.getName());
        assertEquals("kavindu@example.com", user.getEmail());
        assertEquals("$2a$12$e8Y4JgKz7Q3bY9v1...", user.getPasswordHash());
        assertEquals("+94771234567", user.getPhone());
        assertEquals(Role.PASSENGER, user.getRole());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("Should support getters and setters for all fields")
    void testSettersAndGetters() {
        User user = new User();
        user.setId("user123");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("hashed_secret");
        user.setPhone("+1234567890");
        user.setRole(Role.DRIVER);
        user.setStatus(AccountStatus.INACTIVE);

        assertEquals("user123", user.getId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("hashed_secret", user.getPasswordHash());
        assertEquals("+1234567890", user.getPhone());
        assertEquals(Role.DRIVER, user.getRole());
        assertEquals(AccountStatus.INACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("Should verify equals and hashCode contracts")
    void testEqualsAndHashCode() {
        User user1 = new User("id1", "Alice", "Smith", "alice@example.com", "hash1", "+111", Role.PASSENGER, AccountStatus.ACTIVE);
        User user2 = new User("id1", "Alice", "Smith", "alice@example.com", "hash2", "+222", Role.PASSENGER, AccountStatus.ACTIVE);
        User user3 = new User("id2", "Bob", "Jones", "bob@example.com", "hash3", "+333", Role.DRIVER, AccountStatus.ACTIVE);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertFalse(user1.equals(user3));
        assertFalse(user1.equals(null));
        assertFalse(user1.equals(new Object()));
    }

    @Test
    @DisplayName("Should not expose passwordHash in toString()")
    void testToStringSecurity() {
        User user = new User("id1", "Kavindu", "Umayanga", "kavindu@example.com", "super_secret_hash", "+94771234567", Role.PASSENGER, AccountStatus.ACTIVE);
        String stringRepresentation = user.toString();

        assertFalse(stringRepresentation.contains("super_secret_hash"), "toString() must not expose passwordHash");
        assertTrue(stringRepresentation.contains("kavindu@example.com"));
        assertTrue(stringRepresentation.contains("PASSENGER"));
    }

    @Test
    @DisplayName("Should have appropriate MongoDB annotations")
    void testMongoAnnotations() throws NoSuchFieldException {
        Document docAnnotation = User.class.getAnnotation(Document.class);
        assertNotNull(docAnnotation, "User class should have @Document annotation");
        assertEquals("users", docAnnotation.collection(), "Collection name should be 'users'");

        Field idField = User.class.getDeclaredField("id");
        assertNotNull(idField.getAnnotation(Id.class), "id field should have @Id annotation");

        Field emailField = User.class.getDeclaredField("email");
        Indexed indexedAnnotation = emailField.getAnnotation(Indexed.class);
        assertNotNull(indexedAnnotation, "email field should have @Indexed annotation");
        assertTrue(indexedAnnotation.unique(), "email index should be unique");
    }
}
