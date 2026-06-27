package com.splitease.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Integer fromUserId;
    private Integer toUserId;
    private BigDecimal amount;
}
