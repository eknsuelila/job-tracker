package com.portfolio.jobtracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link User}.
 *
 * <p>This is only an INTERFACE: Spring Data JPA generates the implementation at startup.
 * Extending {@code JpaRepository<User, Long>} provides save(), findById(), findAll(), delete(), count()...
 *
 * <p>Extra queries are written as method names that Spring Data parses ("derived queries"):
 * {@code findByEmailIgnoreCase} becomes {@code SELECT ... FROM users WHERE LOWER(email) = LOWER(?)}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** {@link Optional} forces the caller to handle "not found" explicitly instead of getting null. */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Cheaper than loading the whole user when we only need to know if the email is taken. */
    boolean existsByEmailIgnoreCase(String email);
}
