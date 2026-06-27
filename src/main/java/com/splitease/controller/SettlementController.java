package com.splitease.controller;

import com.splitease.dto.request.SettlementCreateRequest;
import com.splitease.dto.response.SettlementResponse;
import com.splitease.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{id}/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(
            @PathVariable("id") Integer id,
            @Valid @RequestBody SettlementCreateRequest request,
            Principal principal) {
        SettlementResponse response = settlementService.createSettlement(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getSettlements(@PathVariable("id") Integer id, Principal principal) {
        List<SettlementResponse> response = settlementService.getSettlementsForGroup(id, principal.getName());
        return ResponseEntity.ok(response);
    }
}
