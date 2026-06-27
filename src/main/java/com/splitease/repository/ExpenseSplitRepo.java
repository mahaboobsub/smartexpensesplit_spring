package com.splitease.repository;

import com.splitease.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseSplitRepo extends JpaRepository<ExpenseSplit, Integer> {
    List<ExpenseSplit> findByExpenseExpenseId(Integer expenseId);

    @Query("SELECT es.user.userId, SUM(es.share) FROM ExpenseSplit es WHERE es.expense.group.groupId = :groupId GROUP BY es.user.userId")
    List<Object[]> sumExpenseSplitsForGroupMembers(@Param("groupId") Integer groupId);
}
