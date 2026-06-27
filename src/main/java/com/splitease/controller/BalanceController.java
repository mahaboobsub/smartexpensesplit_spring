package com.splitease.controller;

import com.splitease.dto.response.BalanceResponse;
import com.splitease.service.BalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/groups/{id}/balances")
public class BalanceController {

    @Autowired
    private BalanceService balanceService;

    @GetMapping
    public ResponseEntity<BalanceResponse> getBalances(@PathVariable("id") Integer id, Principal principal) {
        BalanceResponse response = balanceService.getBalancesAndSimplification(id, principal.getName());
        return ResponseEntity.ok(response);
    }
}
