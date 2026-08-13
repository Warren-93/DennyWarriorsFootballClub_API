package mark.warren93.dev.DennyWarriorsAPI.exception;

import jakarta.servlet.http.HttpServletRequest;
import mark.warren93.dev.DennyWarriorsAPI.service.JwtService;
import mark.warren93.dev.DennyWarriorsAPI.service.LoginRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return body(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(JwtService.InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(JwtService.InvalidTokenException exception, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(LoginRateLimiter.TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyAttempts(LoginRateLimiter.TooManyAttemptsException exception, HttpServletRequest request) {
        return body(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException exception, HttpServletRequest request) {
        return body(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidMediaException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMedia(InvalidMediaException exception, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidSyncSettingsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSyncSettings(InvalidSyncSettingsException exception, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidUserOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidUserOperation(InvalidUserOperationException exception, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST, message, request);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message,
                "path", request.getRequestURI()
        ));
    }
}
