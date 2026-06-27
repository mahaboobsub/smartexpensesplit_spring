package com.splitease.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private List<MemberBalance> netBalances;
    private List<SimplifiedTransaction> simplifiedSettlements;

    @Data
    @AllArgsConstructor
    public static class MemberBalance {
        private Integer userId;
        private String userName;
        private BigDecimal balance;
    }

    @Data
    @AllArgsConstructor
    public static class SimplifiedTransaction {
        private Integer fromUserId;
        private String fromUserName;
        private Integer toUserId;
        private String toUserName;
        private BigDecimal amount;
    }
}
