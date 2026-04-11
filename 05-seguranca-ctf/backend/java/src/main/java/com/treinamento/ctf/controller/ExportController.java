package com.treinamento.ctf.controller;

import com.treinamento.ctf.middleware.RequestLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @GetMapping("/{format}")
    public ResponseEntity<?> exportTemplate(@PathVariable String format,
                                            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Token nao fornecido");
            return ResponseEntity.status(401).body(err);
        }

        Path templatePath = Paths.get(System.getProperty("user.dir"), "templates", format);

        String template;
        try {
            template = Files.readString(templatePath);
        } catch (IOException ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Template '" + format + "' nao encontrado");
            return ResponseEntity.status(404).body(err);
        }

        RequestLogFilter.CURRENT_SQL.set("");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", format);
        result.put("template", template);
        result.put("message", "Template carregado");

        return ResponseEntity.ok(result);
    }
}
