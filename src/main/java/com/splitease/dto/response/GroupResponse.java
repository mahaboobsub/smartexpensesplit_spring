package com.splitease.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private Integer groupId;
    private String groupName;
    private String description;
    private Integer createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdAt;
    private List<MemberInfo> members;

    @Data
    @AllArgsConstructor
    public static class MemberInfo {
        private Integer userId;
        private String fullName;
        private String email;
    }
}
