package com.nextask.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nextask.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByEmail(String email);
}
