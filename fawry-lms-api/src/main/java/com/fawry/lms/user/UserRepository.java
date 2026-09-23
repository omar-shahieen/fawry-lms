package com.fawry.lms.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.user.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}