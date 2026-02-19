package com.uniqdata.backend.project;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 연구(프로젝트) 엔티티.
 * Wireframe 상태: draft, recruiting, collecting, analyzing, completed
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    /** 참여 시 예치할 XRP (에스크로 금액) */
    private Long escrowAmountXrp;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum ProjectStatus {
        DRAFT,       // 초안
        RECRUITING,  // 모집 중
        COLLECTING,  // 수집 중
        ANALYZING,   // 분석 중
        COMPLETED    // 완료
    }
}
