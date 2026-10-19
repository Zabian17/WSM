package com.mycompany.tugas_akhir.api.mobile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MobileApiAuthenticationController.class)
public class MobileApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MobileApiExceptionHandler.class);

    @ExceptionHandler(MobileApiExceptions.InvalidCredentials.class)
    ResponseEntity<MobileApiError> invalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email atau password tidak valid.");
    }

    @ExceptionHandler(MobileApiExceptions.RateLimited.class)
    ResponseEntity<MobileApiError> rateLimited() {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Terlalu banyak percobaan masuk. Coba lagi setelah 15 menit.");
    }

    @ExceptionHandler(MobileApiExceptions.NotFound.class)
    ResponseEntity<MobileApiError> notFound() {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Data tidak ditemukan.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<MobileApiError> unreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Isi permintaan tidak valid.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<MobileApiError> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<MobileApiError> stateConflict(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<MobileApiError> unexpected(Exception exception) {
        log.error("Permintaan mobile API gagal", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Permintaan gagal diproses.");
    }

    private ResponseEntity<MobileApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new MobileApiError(
                new MobileApiError.ErrorDetail(code, message == null ? "Permintaan tidak valid." : message)));
    }
}
