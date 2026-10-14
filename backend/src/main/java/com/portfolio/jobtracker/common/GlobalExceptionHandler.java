package com.portfolio.jobtracker.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handling for all controllers.
 *
 * <p>{@code @RestControllerAdvice} makes this class apply to every {@code @RestController}. When a
 * controller or service throws, Spring looks for the {@code @ExceptionHandler} method that matches the
 * exception type and uses its return value as the HTTP response. Thanks to this:
 * <ul>
 *   <li>services just throw meaningful exceptions ({@link ResourceNotFoundException}...) and don't know about HTTP;</li>
 *   <li>every error has the same JSON shape ({@link ApiError}), so the frontend handles errors in one place;</li>
 *   <li>stack traces are never leaked to clients.</li>
 * </ul>
 *
 * <pre>
 * Exception                              HTTP status
 * MethodArgumentNotValidException (@Valid failed)   400 + fieldErrors
 * HttpMessageNotReadable / type mismatch            400
 * BadCredentialsException                           401
 * ResourceNotFoundException                         404
 * ConflictException                                 409
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ApiError body = new ApiError(Instant.now(), 400, "Bad Request", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadInput(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
