package com.clubsite.api.auth;

import com.clubsite.api.config.ClubProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issues and validates HS256 JWTs using only the JDK and Jackson.
 *
 * Token format: base64url(header) . base64url(claims) . base64url(HMAC-SHA256(header.claims))
 *
 * Two token types share the format and are told apart by the "type" claim:
 * short-lived "access" tokens authenticate requests, longer-lived "refresh"
 * tokens are only accepted by POST /api/v1/auth/refresh.
 */
@Service
public class JwtService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    // HS256 needs a key of at least 256 bits.
    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secretBytes;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String issuer;

    @Autowired
    public JwtService(ClubProperties properties) {
        this(properties.security().jwt());
    }

    public JwtService(ClubProperties.Jwt jwt) {
        if (jwt.secret() == null || jwt.secret().getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to at least " + MIN_SECRET_BYTES + " characters");
        }
        this.secretBytes = jwt.secret().getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtl = jwt.accessTokenTtl();
        this.refreshTokenTtl = jwt.refreshTokenTtl();
        this.issuer = jwt.issuer();
    }

    public IssuedToken generateAccessToken(String username, Role role) {
        return generateToken(username, role, TYPE_ACCESS, accessTokenTtl);
    }

    public IssuedToken generateRefreshToken(String username, Role role) {
        return generateToken(username, role, TYPE_REFRESH, refreshTokenTtl);
    }

    private IssuedToken generateToken(String username, Role role, String type, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", username);
        claims.put("role", role.name());
        claims.put("type", type);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String signingInput = encodeJson(header) + "." + encodeJson(claims);
        String token = signingInput + "." + B64_URL.encodeToString(sign(signingInput));
        return new IssuedToken(token, expiresAt);
    }

    /** Verifies signature, issuer and expiry. Does not check the token type. */
    public Map<String, Object> parse(String token) {
        if (token == null) {
            throw new InvalidTokenException("Missing token");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Malformed JWT");
        }

        byte[] expectedSig = sign(parts[0] + "." + parts[1]);
        byte[] actualSig;
        try {
            actualSig = B64_URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid signature encoding", ex);
        }

        // MessageDigest.isEqual is constant-time, so signature checks don't leak timing.
        if (!java.security.MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new InvalidTokenException("Signature mismatch");
        }

        Map<String, Object> claims = decodeJson(parts[1]);

        if (!issuer.equals(claims.get("iss"))) {
            throw new InvalidTokenException("Invalid issuer");
        }

        if (!(claims.get("exp") instanceof Number exp) || Instant.now().getEpochSecond() >= exp.longValue()) {
            throw new InvalidTokenException("Token expired");
        }

        return claims;
    }

    public Map<String, Object> parseAccessToken(String token) {
        return parseOfType(token, TYPE_ACCESS);
    }

    public Map<String, Object> parseRefreshToken(String token) {
        return parseOfType(token, TYPE_REFRESH);
    }

    private Map<String, Object> parseOfType(String token, String type) {
        Map<String, Object> claims = parse(token);
        if (!type.equals(claims.get("type"))) {
            throw new InvalidTokenException("Not an " + type + " token");
        }
        return claims;
    }

    private byte[] sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC signing failed", ex);
        }
    }

    private static String encodeJson(Map<String, Object> value) {
        try {
            return B64_URL.encodeToString(MAPPER.writeValueAsBytes(value));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not serialise JWT segment", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodeJson(String segment) {
        try {
            return MAPPER.readValue(B64_URL_DECODER.decode(segment), Map.class);
        } catch (IllegalArgumentException | IOException ex) {
            throw new InvalidTokenException("Could not parse JWT payload", ex);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    /** Bad signature, malformed shape, wrong issuer, wrong type, or expired. */
    public static class InvalidTokenException extends org.springframework.security.core.AuthenticationException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
