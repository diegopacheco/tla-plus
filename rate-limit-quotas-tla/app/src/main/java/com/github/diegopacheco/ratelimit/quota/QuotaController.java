package com.github.diegopacheco.ratelimit.quota;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotas")
public class QuotaController {

    private final AtomicQuotaService service;

    public QuotaController(AtomicQuotaService service) {
        this.service = service;
    }

    @PostMapping("/{tenant}/consume")
    public ResponseEntity<QuotaStatus> consume(@PathVariable String tenant) {
        HttpStatus status = service.consume(tenant) ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status).body(service.status(tenant));
    }

    @GetMapping("/{tenant}")
    public QuotaStatus status(@PathVariable String tenant) {
        return service.status(tenant);
    }
}
