package com.uniqdata.backend.project;

import lombok.Data;

@Data
public class ProjectUpdateDto {
    private String title;
    private String description;
    private Project.ProjectStatus status;
    private Long escrowAmountXrp;
}
