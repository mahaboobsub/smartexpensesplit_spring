package com.splitease.service;

import com.splitease.dto.request.ExpenseCreateRequest;
import com.splitease.dto.response.ExpenseResponse;
import com.splitease.model.*;
import com.splitease.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private UserRepo userRepo;

    public ExpenseResponse addExpense(Integer groupId, ExpenseCreateRequest request, String email) {
        User caller = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Caller not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if caller is member
        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, caller.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        // Check if paidBy user is member
        User paidByUser = userRepo.findById(request.getPaidBy())
                .orElseThrow(() -> new IllegalArgumentException("Paid-by user not found"));
        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, paidByUser.getUserId())) {
            throw new IllegalArgumentException("Paid-by user must be a member of the group");
        }

        // Check if all split users are members
        List<User> splitUsers = new ArrayList<>();
        for (Integer userId : request.getSplitAmong()) {
            User u = userRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Split user not found: " + userId));
            if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, u.getUserId())) {
                throw new IllegalArgumentException("All split-among members must belong to the group");
            }
            splitUsers.add(u);
        }

        // Save Expense
        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidByUser)
                .description(request.getDescription())
                .totalAmount(request.getTotalAmount())
                .expenseDate(request.getDate())
                .build();
        expense = expenseRepo.save(expense);

        // Split equally and handle rounding remainders
        int numMembers = splitUsers.size();
        BigDecimal amount = request.getTotalAmount();
        BigDecimal equalShare = amount.divide(BigDecimal.valueOf(numMembers), 2, RoundingMode.DOWN);
        BigDecimal totalDistributed = equalShare.multiply(BigDecimal.valueOf(numMembers));
        BigDecimal remainder = amount.subtract(totalDistributed);

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < numMembers; i++) {
            User u = splitUsers.get(i);
            BigDecimal share = equalShare;
            // The first user gets the remainder to keep the sum exact
            if (i == 0) {
                share = share.add(remainder);
            }

            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .user(u)
                    .share(share)
                    .build();
            splits.add(expenseSplitRepo.save(split));
        }

        return mapToExpenseResponse(expense, splits);
    }

    public List<ExpenseResponse> getExpensesForGroup(Integer groupId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, user.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        List<Expense> expenses = expenseRepo.findByGroupGroupIdOrderByExpenseDateDesc(groupId);
        return expenses.stream().map(this::mapToExpenseResponse).collect(Collectors.toList());
    }

    public void deleteExpense(Integer groupId, Integer expenseId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Only group admin (creator) can delete group expenses
        if (!group.getCreatedBy().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Access denied: Only group admin can delete expenses");
        }

        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (!expense.getGroup().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("Expense does not belong to this group");
        }

        // Delete split associations first
        List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseExpenseId(expenseId);
        expenseSplitRepo.deleteAll(splits);

        // Delete the expense
        expenseRepo.delete(expense);
    }

    public ExpenseResponse mapToExpenseResponse(Expense expense) {
        List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseExpenseId(expense.getExpenseId());
        return mapToExpenseResponse(expense, splits);
    }

    private ExpenseResponse mapToExpenseResponse(Expense expense, List<ExpenseSplit> splits) {
        List<ExpenseResponse.SplitInfo> splitInfos = splits.stream()
                .map(s -> new ExpenseResponse.SplitInfo(s.getUser().getUserId(), s.getUser().getFullName(), s.getShare()))
                .collect(Collectors.toList());

        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .groupId(expense.getGroup().getGroupId())
                .paidByUserId(expense.getPaidBy().getUserId())
                .paidByUserName(expense.getPaidBy().getFullName())
                .description(expense.getDescription())
                .totalAmount(expense.getTotalAmount())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .splits(splitInfos)
                .build();
    }
}
