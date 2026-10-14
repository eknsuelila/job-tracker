package com.portfolio.jobtracker.auth;

import com.portfolio.jobtracker.auth.dto.AuthResponse;
import com.portfolio.jobtracker.auth.dto.LoginRequest;
import com.portfolio.jobtracker.auth.dto.RegisterRequest;
import com.portfolio.jobtracker.auth.dto.UserResponse;
import com.portfolio.jobtracker.common.ConflictException;
import com.portfolio.jobtracker.security.JwtService;
import com.portfolio.jobtracker.user.User;
import com.portfolio.jobtracker.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for registering and logging in.
 *
 * <p>Dependencies arrive through the constructor ("constructor injection"): Spring sees the
 * parameters and passes in the matching beans. This makes the class easy to unit test, because a
 * test can call the constructor with mocks.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates the account and immediately returns a token, so the user is logged in after signing up.
     *
     * <p>{@code @Transactional}: all database work in this method runs in ONE transaction; if any
     * exception is thrown, everything is rolled back.
     *
     * @throws ConflictException (mapped to HTTP 409) if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Normalize the email so "Jane@Mail.com" and "jane@mail.com" are the same account.
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        // Hash the password BEFORE it touches the database.
        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /**
     * Checks the credentials and returns a fresh token.
     *
     * <p>The same error message is used for "unknown email" and "wrong password" on purpose:
     * a different message would let attackers discover which emails have accounts.
     *
     * @throws BadCredentialsException (mapped to HTTP 401) if the email/password pair is wrong
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                // matches() hashes the submitted password with the stored salt and compares the hashes.
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
