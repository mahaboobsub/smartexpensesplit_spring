package com.splitease.service;

import com.splitease.dto.request.SettlementCreateRequest;
import com.splitease.dto.response.SettlementResponse;
import com.splitease.model.Group;
import com.splitease.model.Settlement;
import com.splitease.model.User;
import com.splitease.repository.GroupMemberRepo;
import com.splitease.repository.GroupRepo;
import com.splitease.repository.SettlementRepo;
import com.splitease.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SettlementService {

    @Autowired
    private SettlementRepo settlementRepo;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private UserRepo userRepo;

    public SettlementResponse createSettlement(Integer groupId, SettlementCreateRequest request, String email) {
        User caller = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Caller not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, caller.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        User payer = userRepo.findById(request.getFromUserId())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        User receiver = userRepo.findById(request.getToUserId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, payer.getUserId())) {
            throw new IllegalArgumentException("Payer must be a member of this group");
        }
        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, receiver.getUserId())) {
            throw new IllegalArgumentException("Payee must be a member of this group");
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .paidBy(payer)
                .paidTo(receiver)
                .amount(request.getAmount())
                .note(request.getNote())
                .build();

        settlement = settlementRepo.save(settlement);

        return mapToSettlementResponse(settlement);
    }

    public List<SettlementResponse> getSettlementsForGroup(Integer groupId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (!groupMemberRepo.existsByGroupGroupIdAndUserUserId(groupId, user.getUserId())) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        List<Settlement> settlements = settlementRepo.findByGroupGroupIdOrderBySettledAtDesc(groupId);
        return settlements.stream().map(this::mapToSettlementResponse).collect(Collectors.toList());
    }

    public SettlementResponse mapToSettlementResponse(Settlement s) {
        return SettlementResponse.builder()
                .settlementId(s.getSettlementId())
                .groupId(s.getGroup().getGroupId())
                .paidByUserId(s.getPaidBy().getUserId())
                .paidByUserName(s.getPaidBy().getFullName())
                .paidToUserId(s.getPaidTo().getUserId())
                .paidToUserName(s.getPaidTo().getFullName())
                .amount(s.getAmount())
                .note(s.getNote())
                .settledAt(s.getSettledAt())
                .build();
    }
}
