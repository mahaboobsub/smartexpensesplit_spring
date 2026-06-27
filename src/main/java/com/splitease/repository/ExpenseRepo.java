package com.splitease.repository;

import com.splitease.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Integer> {
    List<Expense> findByGroupGroupIdOrderByExpenseDateDesc(Integer groupId);

    @Query("SELECT e.paidBy.userId, SUM(e.totalAmount) FROM Expense e WHERE e.group.groupId = :groupId GROUP BY e.paidBy.userId")
    List<Object[]> sumExpensesPaidByGroupMembers(@Param("groupId") Integer groupId);
}
