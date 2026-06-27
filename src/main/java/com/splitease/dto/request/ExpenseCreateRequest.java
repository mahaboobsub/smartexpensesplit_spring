package com.splitease.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseCreateRequest {
    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotNull(message = "Paid-by user ID is required")
    private Integer paidBy;

    @NotEmpty(message = "Split-among list cannot be empty")
    private List<Integer> splitAmong;

    @NotNull(message = "Date is required")
    private LocalDate date;
}
