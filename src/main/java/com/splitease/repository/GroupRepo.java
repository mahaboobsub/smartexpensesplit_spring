package com.splitease.repository;

import com.splitease.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepo extends JpaRepository<Group, Integer> {
    @Query("SELECT g FROM Group g JOIN GroupMember gm ON g.groupId = gm.group.groupId WHERE gm.user.userId = :userId")
    List<Group> findAllByUserId(@Param("userId") Integer userId);
}
