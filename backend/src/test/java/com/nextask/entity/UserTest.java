package com.nextask.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createsUserWithNormalizedTextFields() {
        User user = new User(
                "  Ada Lovelace  ",
                "  ADA.LOVELACE@Example.COM  ",
                "  encoded-password-hash  ");

        assertThat(user.getName()).isEqualTo("Ada Lovelace");
        assertThat(user.getEmail()).isEqualTo("ada.lovelace@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password-hash");
        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> new User(" ", "ada@example.com", "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");

        assertThatThrownBy(() -> new User("Ada", null, "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");

        assertThatThrownBy(() -> new User("Ada", "ada@example.com", "\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not be blank");
    }

    @Test
    void rejectsValuesThatExceedDatabaseColumnLengths() {
        assertThatThrownBy(() -> new User("a".repeat(121), "ada@example.com", "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must have at most 120 characters");

        assertThatThrownBy(() -> new User("Ada", "a".repeat(255), "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must have at most 254 characters");

        assertThatThrownBy(() -> new User("Ada", "ada@example.com", "h".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must have at most 255 characters");
    }

    @Test
    void reportsMalformedEmailThroughBeanValidation() {
        User user = new User("Ada", "not-an-email", "encoded-hash");

        Set<String> invalidProperties = validator.validate(user).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(invalidProperties).containsExactly("email");
    }

    @Test
    void changesMutableDataThroughExplicitOperations() {
        User user = new User("Ada", "ada@example.com", "old-hash");

        user.rename("  Augusta Ada King  ");
        user.changeEmail("  ADA.KING@EXAMPLE.COM  ");
        user.changePasswordHash("  new-hash  ");

        assertThat(user.getName()).isEqualTo("Augusta Ada King");
        assertThat(user.getEmail()).isEqualTo("ada.king@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }
}
