package mark.warren93.dev.DennyWarriorsAPI.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issues and validates HS256 JWT bearer tokens using only the JDK + Jackson.
 * No third-party JWT library required.
 *
 * Token format: base64url(header) . base64url(payload) . base64url(HMAC-SHA256(header.payload))
 */
@Service
public class JwtService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secretBytes;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${dwfc.jwt.secret}") String secret,
            @Value("${dwfc.jwt.expiration-ms}") long expirationMs,
            @Value("${dwfc.jwt.issuer}") String issuer) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(String username, boolean admin) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + (expirationMs / 1000);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", username);
        claims.put("admin", admin);
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        String headerB64 = encodeJson(header);
        String claimsB64 = encodeJson(claims);
        String signingInput = headerB64 + "." + claimsB64;
        String signatureB64 = B64_URL.encodeToString(sign(signingInput));

        return signingInput + "." + signatureB64;
    }

    public Map<String, Object> parse(String token) {
        if (token == null) {
            throw new InvalidTokenException("Missing token");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Malformed JWT");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSig = sign(signingInput);
        byte[] actualSig;
        try {
            actualSig = B64_URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid signature encoding", ex);
        }

        if (!constantTimeEquals(expectedSig, actualSig)) {
            throw new InvalidTokenException("Signature mismatch");
        }

        Map<String, Object> claims = decodeJson(parts[1]);

        Object iss = claims.get("iss");
        if (!issuer.equals(iss)) {
            throw new InvalidTokenException("Invalid issuer");
        }

        Object exp = claims.get("exp");
        if (exp instanceof Number n && Instant.now().getEpochSecond() >= n.longValue()) {
            throw new InvalidTokenException("Token expired");
        }

        return claims;
    }

    public Instant expiryOf(long fromNowMs) {
        return Instant.now().plusMillis(fromNowMs);
    }

    public long expirationMs() {
        return expirationMs;
    }

    // ---------------------------------------------------------------------

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
            byte[] bytes = B64_URL_DECODER.decode(segment);
            return MAPPER.readValue(bytes, Map.class);
        } catch (IllegalArgumentException | IOException ex) {
            throw new InvalidTokenException("Could not parse JWT payload", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * Thrown when a presented token cannot be trusted — bad signature,
     * malformed shape, wrong issuer, or expired.
     */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
