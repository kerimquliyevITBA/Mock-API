package com.example.mockapi.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogDao dao;

    public AuditLogController(AuditLogDao dao) {
        this.dao = dao;
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        if (limit > 200) limit = 200;
        List<AuditLogDao.AuditEntry> entries = dao.list(username, method, status, from, to, limit, offset);
        long total = dao.count(username, method, status, from, to);

        return Map.of(
                "data", entries,
                "total", total,
                "limit", limit,
                "offset", offset
        );
    }
}
