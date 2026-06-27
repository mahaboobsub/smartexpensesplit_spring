package com.splitease.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementCreateRequest {
    @NotNull(message = "Payer user ID is required")
    private Integer fromUserId;

    @NotNull(message = "Payee user ID is required")
    private Integer toUserId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String note;
}
