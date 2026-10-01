package com.ridelink.account.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    private String firstName;

    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    private Role role;

    private AccountStatus status = AccountStatus.ACTIVE;

    public User() {
        this.status = AccountStatus.ACTIVE;
    }

    public User(String id, String firstName, String lastName, String email, String passwordHash, String phone, Role role, AccountStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = buildFullName(firstName, lastName);
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
        this.status = status != null ? status : AccountStatus.ACTIVE;
    }

    public User(String id, String name, String email, String passwordHash, String phone, Role role, AccountStatus status) {
        this.id = id;
        this.name = name;
        populateFirstAndLastNameFromName(name);
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
        this.status = status != null ? status : AccountStatus.ACTIVE;
    }

    public User(String firstName, String lastName, String email, String passwordHash, String phone, Role role) {
        this(null, firstName, lastName, email, passwordHash, phone, role, AccountStatus.ACTIVE);
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? null : combined;
    }

    private void populateFirstAndLastNameFromName(String fullName) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return buildFullName(firstName, lastName);
    }

    public void setName(String name) {
        this.name = name;
        if (this.firstName == null && this.lastName == null) {
            populateFirstAndLastNameFromName(name);
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.name = buildFullName(this.firstName, this.lastName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.name = buildFullName(this.firstName, this.lastName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + getName() + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}
