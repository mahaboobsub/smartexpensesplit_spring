package com.splitease.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupCreateRequest {
    @NotBlank(message = "Group name is required")
    private String name;
    private String description;
}
