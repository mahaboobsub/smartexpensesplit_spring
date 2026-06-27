package com.splitease.service;

import com.splitease.algorithm.DebtSimplifier;
import com.splitease.algorithm.Transaction;
import com.splitease.dto.response.BalanceResponse;
import com.splitease.model.Group;
import com.splitease.model.GroupMember;
import com.splitease.model.User;
import com.splitease.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private SettlementRepo settlementRepo;

    @Autowired
    private DebtSimplifier debtSimplifier;

    public Map<Integer, BigDecimal> calculateNetBalances(Integer groupId) {
        List<GroupMember> members = groupMemberRepo.findByGroupGroupId(groupId);
        Map<Integer, BigDecimal> netBalances = new HashMap<>();
        for (GroupMember member : members) {
            netBalances.put(member.getUser().getUserId(), BigDecimal.ZERO);
        }

        List<Object[]> expensesPaid = expenseRepo.sumExpensesPaidByGroupMembers(groupId);
        if (expensesPaid != null) {
            for (Object[] row : expensesPaid) {
                Integer userId = (Integer) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                if (userId != null && sum != null && netBalances.containsKey(userId)) {
                    netBalances.put(userId, netBalances.get(userId).add(sum));
                }
            }
        }

        List<Object[]> splitsOwed = expenseSplitRepo.sumExpenseSplitsForGroupMembers(groupId);
        if (splitsOwed != null) {
            for (Object[] row : splitsOwed) {
                Integer userId = (Integer) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                if (userId != null && sum != null && netBalances.containsKey(userId)) {
                    netBalances.put(userId, netBalances.get(userId).subtract(sum));
                }
            }
        }

        List<Object[]> settlementsPaid = settlementRepo.sumSettlementsPaidByGroupMembers(groupId);
        if (settlementsPaid != null) {
            for (Object[] row : settlementsPaid) {
                Integer userId = (Integer) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                if (userId != null && sum != null && netBalances.containsKey(userId)) {
                    netBalances.put(userId, netBalances.get(userId).add(sum));
                }
            }
        }

        List<Object[]> settlementsReceived = settlementRepo.sumSettlementsPaidToGroupMembers(groupId);
        if (settlementsReceived != null) {
            for (Object[] row : settlementsReceived) {
                Integer userId = (Integer) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                if (userId != null && sum != null && netBalances.containsKey(userId)) {
                    netBalances.put(userId, netBalances.get(userId).subtract(sum));
                }
            }
        }

        return netBalances;
    }

    public BalanceResponse getBalancesAndSimplification(Integer groupId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, user.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        Map<Integer, BigDecimal> netBalances = calculateNetBalances(groupId);

        List<GroupMember> members = groupMemberRepo.findByGroupGroupId(groupId);
        Map<Integer, String> namesMap = members.stream()
                .collect(Collectors.toMap(m -> m.getUser().getUserId(), m -> m.getUser().getFullName()));

        List<BalanceResponse.MemberBalance> memberBalancesList = netBalances.entrySet().stream()
                .map(e -> new BalanceResponse.MemberBalance(e.getKey(), namesMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());

        List<Transaction> simplified = debtSimplifier.simplify(netBalances);

        List<BalanceResponse.SimplifiedTransaction> simplifiedList = simplified.stream()
                .map(t -> new BalanceResponse.SimplifiedTransaction(
                        t.getFromUserId(),
                        namesMap.get(t.getFromUserId()),
                        t.getToUserId(),
                        namesMap.get(t.getToUserId()),
                        t.getAmount()
                ))
                .collect(Collectors.toList());

        return BalanceResponse.builder()
                .netBalances(memberBalancesList)
                .simplifiedSettlements(simplifiedList)
                .build();
    }
}
