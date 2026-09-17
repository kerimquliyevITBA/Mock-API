package com.example.mockapi;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        Throwable root = ex.getMostSpecificCause();
        String sqlState = root instanceof SQLException sql ? sql.getSQLState() : null;

        HttpStatus status = switch (sqlState == null ? "" : sqlState) {
            case "23P01" -> HttpStatus.CONFLICT;      // exclusion_violation -> otaq həmin vaxtda dolu
            case "P0001" -> HttpStatus.BAD_REQUEST;   // raise exception -> keçmiş tarix / bitmə<başlama
            default       -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return build(status, cleanMessage(root.getMessage()), sqlState);
    }

    private static String cleanMessage(String raw) {
        if (raw == null) {
            return "Naməlum xəta";
        }
        String msg = raw;
        int nl = msg.indexOf('\n');
        if (nl >= 0) {
            msg = msg.substring(0, nl);
        }
        if (msg.startsWith("ERROR: ")) {
            msg = msg.substring("ERROR: ".length());
        }
        return msg.trim();
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String sqlState) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", message);
        if (sqlState != null) {
            body.put("sqlState", sqlState);
        }
        return ResponseEntity.status(status).body(body);
    }
}
