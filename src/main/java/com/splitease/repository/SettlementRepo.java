package com.splitease.repository;

import com.splitease.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepo extends JpaRepository<Settlement, Integer> {
    List<Settlement> findByGroupGroupIdOrderBySettledAtDesc(Integer groupId);

    @Query("SELECT s.paidBy.userId, SUM(s.amount) FROM Settlement s WHERE s.group.groupId = :groupId GROUP BY s.paidBy.userId")
    List<Object[]> sumSettlementsPaidByGroupMembers(@Param("groupId") Integer groupId);

    @Query("SELECT s.paidTo.userId, SUM(s.amount) FROM Settlement s WHERE s.group.groupId = :groupId GROUP BY s.paidTo.userId")
    List<Object[]> sumSettlementsPaidToGroupMembers(@Param("groupId") Integer groupId);
}
