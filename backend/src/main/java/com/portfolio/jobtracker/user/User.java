package com.portfolio.jobtracker.user;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * A registered user of the application.
 *
 * <p><b>JPA entity:</b> {@code @Entity} tells Hibernate (the JPA implementation) that each instance
 * of this class corresponds to one row in a database table. {@code @Table(name = "users")} names the
 * table explicitly because "user" is a reserved word in PostgreSQL.
 *
 * <p><b>Spring Security principal:</b> implementing {@link UserDetails} lets Spring Security use this
 * object directly as "the currently logged-in user". After the JWT filter authenticates a request,
 * controllers can receive this object with {@code @AuthenticationPrincipal User user}.
 *
 * <p>The table structure itself is created by the Flyway script {@code V1__create_tables.sql};
 * Hibernate only checks ("validates") that this class matches it.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    /**
     * Primary key. {@code IDENTITY} means the database generates the value (auto-increment column),
     * so the id is {@code null} until the entity is saved.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Used as the login name. The UNIQUE constraint makes the database reject duplicates too. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt hash of the password - NEVER the password itself. BCrypt is a one-way, deliberately slow,
     * salted hash: even if the database leaks, the original passwords can't be read back.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** JPA requires a no-argument constructor to build objects from query results. {@code protected}
     *  keeps other code from creating "empty" users by accident. */
    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }

    // ----- UserDetails methods (required by Spring Security) -----

    /** Roles/permissions. This app has a single role; apps with admins would return more here. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** Spring Security calls the stored password "password"; for us it is the BCrypt hash. */
    @Override
    public String getPassword() { return passwordHash; }

    /** The unique login identifier. We log in with email, so that is our "username". */
    @Override
    public String getUsername() { return email; }

    // isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired() and isEnabled() have
    // default implementations in UserDetails that return true, so they don't need overriding here.
}
