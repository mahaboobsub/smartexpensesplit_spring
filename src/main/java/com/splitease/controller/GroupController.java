package com.splitease.controller;

import com.splitease.dto.request.GroupCreateRequest;
import com.splitease.dto.request.MemberInviteRequest;
import com.splitease.dto.response.GroupResponse;
import com.splitease.dto.response.MessageResponse;
import com.splitease.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupCreateRequest request, Principal principal) {
        GroupResponse response = groupService.createGroup(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(Principal principal) {
        List<GroupResponse> response = groupService.getGroupsForUser(principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable("id") Integer id, Principal principal) {
        GroupResponse response = groupService.getGroupById(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MessageResponse> inviteMember(
            @PathVariable("id") Integer id,
            @Valid @RequestBody MemberInviteRequest request,
            Principal principal) {
        groupService.inviteMember(id, request, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Member invited successfully"));
    }

    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<MessageResponse> leaveGroup(@PathVariable("id") Integer id, Principal principal) {
        groupService.leaveGroup(id, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Successfully left the group"));
    }
}
