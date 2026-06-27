package com.splitease.service;

import com.splitease.dto.request.GroupCreateRequest;
import com.splitease.dto.request.MemberInviteRequest;
import com.splitease.dto.response.GroupResponse;
import com.splitease.model.Group;
import com.splitease.model.GroupMember;
import com.splitease.model.User;
import com.splitease.repository.GroupMemberRepo;
import com.splitease.repository.GroupRepo;
import com.splitease.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    @Lazy
    private BalanceService balanceService;

    public GroupResponse createGroup(GroupCreateRequest request, String email) {
        User creator = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = Group.builder()
                .groupName(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();

        group = groupRepo.save(group);

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(creator)
                .build();
        groupMemberRepo.save(member);

        return mapToGroupResponse(group);
    }

    public List<GroupResponse> getGroupsForUser(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Group> groups = groupRepo.findAllByUserId(user.getUserId());
        return groups.stream().map(this::mapToGroupResponse).collect(Collectors.toList());
    }

    public GroupResponse getGroupById(Integer groupId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, user.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        return mapToGroupResponse(group);
    }

    public void inviteMember(Integer groupId, MemberInviteRequest request, String email) {
        User inviter = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, inviter.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        User invitee = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No user found with email " + request.getEmail()));

        if (groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, invitee.getUserId())) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(invitee)
                .build();
        groupMemberRepo.save(newMember);
    }

    public void leaveGroup(Integer groupId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupMember membership = groupMemberRepo.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        // Enforce balance zero check server-side
        Map<Integer, BigDecimal> netBalances = balanceService.calculateNetBalances(groupId);
        BigDecimal userBalance = netBalances.getOrDefault(user.getUserId(), BigDecimal.ZERO);

        // Compare balance to zero (scale insensitive)
        if (userBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Cannot leave group: Your net balance must be zero. Current balance: " + userBalance);
        }

        groupMemberRepo.delete(membership);
    }

    public GroupResponse mapToGroupResponse(Group group) {
        List<GroupMember> members = groupMemberRepo.findByGroupGroupId(group.getGroupId());
        List<GroupResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new GroupResponse.MemberInfo(m.getUser().getUserId(), m.getUser().getFullName(), m.getUser().getEmail()))
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .createdByUserId(group.getCreatedBy().getUserId())
                .createdByUserName(group.getCreatedBy().getFullName())
                .createdAt(group.getCreatedAt())
                .members(memberInfos)
                .build();
    }
}
