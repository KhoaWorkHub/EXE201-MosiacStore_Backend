package com.mosiacstore.mosiac.web.advice;

import com.mosiacstore.mosiac.application.exception.AuthenticationException;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.exception.ResourceConflictException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "You don't have permission to access this resource"
        );
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleResourceConflictException(ResourceConflictException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT,
                "Resource conflict",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiError> handleInvalidOperationException(InvalidOperationException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Invalid operation",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                "File upload error",
                "Maximum upload size exceeded"
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                errors.toString()
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    // General exception handler for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex) {
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiError {
        private HttpStatus status;
        private String message;
        private String debugMessage;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ApiError(HttpStatus status, String message, String debugMessage) {
            this.status = status;
            this.message = message;
            this.debugMessage = debugMessage;
        }
    }
}