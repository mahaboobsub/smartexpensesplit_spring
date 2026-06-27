package com.splitease.repository;

import com.splitease.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMember, Integer> {
    List<GroupMember> findByGroupGroupId(Integer groupId);
    Optional<GroupMember> findByGroupGroupIdAndUserUserId(Integer groupId, Integer userId);
    Optional<GroupMember> findByGroupGroupIdAndUserEmail(Integer groupId, String email);
    boolean existsByGroupGroupIdAndUserUserId(Integer groupId, Integer userId);
    void deleteByGroupGroupIdAndUserUserId(Integer groupId, Integer userId);
}
