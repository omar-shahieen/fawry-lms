package com.fawry.lms.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserAndFindsItByEmail() {
        User user = createUser("student@example.com");

        User savedUser = userRepository.saveAndFlush(user);
        User foundUser = userRepository.findByEmail("student@example.com").orElseThrow();

        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getRole()).isEqualTo(Role.STUDENT);
        assertThat(foundUser.isActive()).isTrue();
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void emailColumnHasDatabaseEnforcedUniqueness() {
        userRepository.saveAndFlush(createUser("same@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(createUser("same@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String email) {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(Role.STUDENT);
        return user;
    }
}