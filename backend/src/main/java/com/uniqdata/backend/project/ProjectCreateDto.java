package com.uniqdata.backend.project;

import lombok.Data;

@Data
public class ProjectCreateDto {
    private String title;
    private String description;
    private Long escrowAmountXrp;
}
