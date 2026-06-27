package com.splitease.algorithm;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class DebtSimplifierTest {

    private final DebtSimplifier debtSimplifier = new DebtSimplifier();

    @Test
    public void testSimplifyNoDebts() {
        Map<Integer, BigDecimal> balances = new HashMap<>();
        balances.put(1, new BigDecimal("0.00"));
        balances.put(2, new BigDecimal("0.00"));
        
        List<Transaction> result = debtSimplifier.simplify(balances);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSimplifyTwoUsers() {
        Map<Integer, BigDecimal> balances = new HashMap<>();
        balances.put(1, new BigDecimal("-50.00")); 
        balances.put(2, new BigDecimal("50.00"));  
        
        List<Transaction> result = debtSimplifier.simplify(balances);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFromUserId());
        assertEquals(2, result.get(0).getToUserId());
        assertEquals(new BigDecimal("50.00"), result.get(0).getAmount());
    }

    @Test
    public void testSimplifyThreeUsersComplex() {
        Map<Integer, BigDecimal> balances = new HashMap<>();
        balances.put(1, new BigDecimal("-200.00")); 
        balances.put(2, new BigDecimal("0.00"));    
        balances.put(3, new BigDecimal("200.00"));   
        
        List<Transaction> result = debtSimplifier.simplify(balances);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFromUserId());
        assertEquals(3, result.get(0).getToUserId());
        assertEquals(new BigDecimal("200.00"), result.get(0).getAmount());
    }

    @Test
    public void testSimplifyFourUsersComplex() {
        Map<Integer, BigDecimal> balances = new HashMap<>();
        balances.put(1, new BigDecimal("-100.00"));
        balances.put(2, new BigDecimal("-300.00"));
        balances.put(3, new BigDecimal("250.00"));
        balances.put(4, new BigDecimal("150.00"));
        
        List<Transaction> result = debtSimplifier.simplify(balances);
        assertEquals(3, result.size());
        
        BigDecimal totalSettleAmount = BigDecimal.ZERO;
        for (Transaction t : result) {
            totalSettleAmount = totalSettleAmount.add(t.getAmount());
        }
        assertEquals(new BigDecimal("400.00"), totalSettleAmount);
    }
}
