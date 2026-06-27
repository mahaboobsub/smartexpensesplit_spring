package com.splitease.controller;

import com.splitease.dto.request.ExpenseCreateRequest;
import com.splitease.dto.response.ExpenseResponse;
import com.splitease.dto.response.MessageResponse;
import com.splitease.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{id}/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ExpenseCreateRequest request,
            Principal principal) {
        ExpenseResponse response = expenseService.addExpense(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(@PathVariable("id") Integer id, Principal principal) {
        List<ExpenseResponse> response = expenseService.getExpensesForGroup(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expId}")
    public ResponseEntity<MessageResponse> deleteExpense(
            @PathVariable("id") Integer id,
            @PathVariable("expId") Integer expId,
            Principal principal) {
        expenseService.deleteExpense(id, expId, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Expense deleted successfully"));
    }
}
