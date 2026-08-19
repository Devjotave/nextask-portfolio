package com.nextask.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users", schema = "dbo")
public class User {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PASSWORD_HASH_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = MAX_NAME_LENGTH)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = MAX_EMAIL_LENGTH)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank
    @Size(max = MAX_PASSWORD_HASH_LENGTH)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {
        // Required by JPA.
    }

    public User(String name, String email, String passwordHash) {
        rename(name);
        changeEmail(email);
        changePasswordHash(passwordHash);
    }

    public void rename(String name) {
        this.name = requireText(name, "name", MAX_NAME_LENGTH);
    }

    public void changeEmail(String email) {
        this.email = requireText(email, "email", MAX_EMAIL_LENGTH)
                .toLowerCase(Locale.ROOT);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = requireText(
                passwordHash,
                "passwordHash",
                MAX_PASSWORD_HASH_LENGTH);
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        String normalizedValue = value.strip();
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must have at most " + maxLength + " characters");
        }

        return normalizedValue;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
