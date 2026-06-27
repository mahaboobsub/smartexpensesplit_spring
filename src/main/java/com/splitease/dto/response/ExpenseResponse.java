package com.splitease.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Integer expenseId;
    private Integer groupId;
    private Integer paidByUserId;
    private String paidByUserName;
    private String description;
    private BigDecimal totalAmount;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private List<SplitInfo> splits;

    @Data
    @AllArgsConstructor
    public static class SplitInfo {
        private Integer userId;
        private String userName;
        private BigDecimal share;
    }
}
