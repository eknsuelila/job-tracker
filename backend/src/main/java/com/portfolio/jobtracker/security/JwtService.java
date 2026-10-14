package com.portfolio.jobtracker.security;

import com.portfolio.jobtracker.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates JSON Web Tokens (JWTs).
 *
 * <p><b>What is a JWT?</b> A string of three Base64 parts separated by dots:
 * {@code header.payload.signature}.
 * <ul>
 *   <li>header - the signing algorithm, e.g. {@code {"alg":"HS384"}}</li>
 *   <li>payload ("claims") - data such as {@code {"sub":"demo@example.com","name":"Demo User","exp":1760000000}}</li>
 *   <li>signature - an HMAC of header + payload computed with a SECRET key only the server knows</li>
 * </ul>
 * Anyone can READ the payload (it is only encoded, not encrypted), so never put secrets in it.
 * But nobody can CHANGE it without the secret key, because the signature would no longer match.
 * That is what lets the server trust the token without storing sessions: the app is "stateless".
 *
 * <p>{@code @Service} registers this class as a Spring bean so it can be injected elsewhere.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * {@code @Value("${...}")} injects values from application.yml. In production the secret comes from
     * the JWT_SECRET environment variable so it is never committed to Git.
     */
    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // HMAC-SHA keys must be at least 256 bits; hmacShaKeyFor() throws at startup if the secret is too short.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    /** Builds a signed token for the user, valid for {@code app.jwt.expiration-ms} (24 h by default). */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())                 // "sub" claim: who this token belongs to
                .claim("name", user.getName())            // custom claim (handy for clients, not used for security)
                .issuedAt(now)                            // "iat" claim
                .expiration(new Date(now.getTime() + expirationMs)) // "exp" claim: rejected after this time
                .signWith(key)                            // algorithm picked from the key size (HS256/384/512)
                .compact();                               // serialize to "xxxxx.yyyyy.zzzzz"
    }

    /**
     * Verifies the signature and expiry, then returns the email (subject) stored in the token.
     *
     * @throws JwtException if the token was tampered with, is malformed or has expired
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)   // this is where signature + expiration are checked
                .getPayload()
                .getSubject();
    }
}
