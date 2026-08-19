package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class UserPersistenceIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReadsUserWithDatabaseGeneratedIdentity() {
        String uniqueEmail = "PHASE2-" + UUID.randomUUID() + "@Example.COM";
        User user = new User("  Phase 2 User  ", uniqueEmail, "encoded-password-hash");

        entityManager.persist(user);
        entityManager.flush();

        Long generatedId = user.getId();
        entityManager.clear();

        User storedUser = entityManager.find(User.class, generatedId);

        assertThat(generatedId).isPositive();
        assertThat(storedUser).isNotNull();
        assertThat(storedUser.getName()).isEqualTo("Phase 2 User");
        assertThat(storedUser.getEmail()).isEqualTo(uniqueEmail.toLowerCase(Locale.ROOT));
        assertThat(storedUser.getPasswordHash()).isEqualTo("encoded-password-hash");
        assertThat(storedUser.getCreatedAt()).isNotNull();
        assertThat(storedUser.getUpdatedAt()).isEqualTo(storedUser.getCreatedAt());
        assertThat(storedUser.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
