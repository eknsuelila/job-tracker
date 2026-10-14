package com.portfolio.jobtracker.auth;

import com.portfolio.jobtracker.auth.dto.AuthResponse;
import com.portfolio.jobtracker.auth.dto.LoginRequest;
import com.portfolio.jobtracker.auth.dto.RegisterRequest;
import com.portfolio.jobtracker.auth.dto.UserResponse;
import com.portfolio.jobtracker.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP endpoints for authentication.
 *
 * <p>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}: return values are
 * serialized to JSON by Jackson instead of being treated as HTML view names.
 * {@code @RequestMapping("/api/auth")} is the URL prefix for every method in this class.
 *
 * <p>Controllers stay thin: they translate HTTP into method calls and back. Business logic lives
 * in {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register -> 201 Created + {token, user}.
     *
     * <p>{@code @RequestBody} converts the JSON body into a RegisterRequest object.
     * {@code @Valid} runs the Bean Validation annotations on it (@NotBlank, @Email...). If any fail,
     * the method is never called and GlobalExceptionHandler returns 400 with the field errors.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** POST /api/auth/login -> 200 OK + {token, user}, or 401 if the credentials are wrong. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * GET /api/auth/me -> the current user. {@code @AuthenticationPrincipal} injects the User that
     * JwtAuthenticationFilter stored in the security context for this request.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }
}
