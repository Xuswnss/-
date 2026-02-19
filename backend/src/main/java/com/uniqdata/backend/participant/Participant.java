package com.uniqdata.backend.participant;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 연구 참여자. 참여 시 Core를 통해 에스크로 생성, 철회 시 취소.
 */
@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project_id", "participant_address" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** 참여자 XRPL 지갑 주소 */
    @Column(name = "participant_address", nullable = false)
    private String participantAddress;

    /** Core createEscrow 응답 — 취소 시 필요 */
    @Column(name = "escrow_owner_address")
    private String escrowOwnerAddress;

    @Column(name = "offer_sequence")
    private Long offerSequence;

    @Column(name = "escrow_tx_hash")
    private String escrowTxHash;

    @Column(nullable = false)
    private boolean active = true;

    private Instant enrolledAt;
    private Instant withdrawnAt;

    @PrePersist
    void prePersist() {
        if (enrolledAt == null) enrolledAt = Instant.now();
    }
}
