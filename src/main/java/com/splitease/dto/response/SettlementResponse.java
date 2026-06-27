package com.splitease.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private Integer settlementId;
    private Integer groupId;
    private Integer paidByUserId;
    private String paidByUserName;
    private Integer paidToUserId;
    private String paidToUserName;
    private BigDecimal amount;
    private String note;
    private LocalDateTime settledAt;
}
