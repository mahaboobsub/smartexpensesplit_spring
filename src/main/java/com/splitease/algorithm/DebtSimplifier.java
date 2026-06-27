package com.splitease.algorithm;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DebtSimplifier {

    public List<Transaction> simplify(Map<Integer, BigDecimal> netBalance) {
        List<BigDecimal> credits = new ArrayList<>();  
        List<Integer> creditIds = new ArrayList<>();
        List<BigDecimal> debits = new ArrayList<>();  
        List<Integer> debitIds = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> e : netBalance.entrySet()) {
            BigDecimal val = e.getValue();
            if (val == null) continue;
            val = val.setScale(2, RoundingMode.HALF_UP);
            int cmp = val.compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                credits.add(val);
                creditIds.add(e.getKey());
            } else if (cmp < 0) {
                debits.add(val.abs());
                debitIds.add(e.getKey());
            }
        }

        List<Transaction> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < credits.size() && j < debits.size()) {
            BigDecimal creditVal = credits.get(i);
            BigDecimal debitVal = debits.get(j);

            if (creditVal.compareTo(BigDecimal.ZERO) == 0) {
                i++;
                continue;
            }
            if (debitVal.compareTo(BigDecimal.ZERO) == 0) {
                j++;
                continue;
            }

            BigDecimal settle = creditVal.min(debitVal);
            if (settle.compareTo(BigDecimal.ZERO) > 0) {
                result.add(new Transaction(debitIds.get(j), creditIds.get(i), settle));
            }

            credits.set(i, creditVal.subtract(settle));
            debits.set(j, debitVal.subtract(settle));

            if (credits.get(i).compareTo(BigDecimal.ZERO) == 0) {
                i++;
            }
            if (debits.get(j).compareTo(BigDecimal.ZERO) == 0) {
                j++;
            }
        }
        return result;
    }
}
