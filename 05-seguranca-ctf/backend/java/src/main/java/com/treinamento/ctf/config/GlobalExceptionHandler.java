package com.treinamento.ctf.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());

        String stackTrace = ex.getStackTrace() != null
                ? Arrays.stream(ex.getStackTrace())
                        .limit(8)
                        .map(StackTraceElement::toString)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("")
                : "";
        body.put("stack", stackTrace.isEmpty()
                ? java.util.List.of()
                : Arrays.asList(stackTrace.split("\n")));

        return ResponseEntity.status(500).body(body);
    }
}
