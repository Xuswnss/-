package com.uniqdata.backend.participant;

import com.uniqdata.backend.core.CoreClient;
import com.uniqdata.backend.core.CoreClientException;
import com.uniqdata.backend.project.Project;
import com.uniqdata.backend.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 참여 신청 시 Core로 에스크로 생성, 참여 철회 시 Core로 에스크로 취소.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;
    private final CoreClient coreClient;

    /**
     * 연구 참여 신청 — DB 저장 + Core 에스크로 생성
     */
    @Transactional
    public Participant enroll(Long projectId, String participantAddress) {
        log.info("[참여신청] enroll 시작 | projectId={} | participantAddress={}...",
                projectId, participantAddress != null ? participantAddress.substring(0, Math.min(12, participantAddress.length())) + "..." : "null");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("[참여신청] 프로젝트 없음 | projectId={}", projectId);
                    return new IllegalArgumentException("Project not found: " + projectId);
                });

        participantRepository.findByProjectIdAndParticipantAddress(projectId, participantAddress)
                .ifPresent(p -> {
                    log.warn("[참여신청] 이미 참여 중 | projectId={} | participantAddress={}...", projectId,
                            participantAddress != null ? participantAddress.substring(0, Math.min(12, participantAddress.length())) + "..." : "null");
                    throw new IllegalArgumentException("Already enrolled");
                });

        long amountXrp = project.getEscrowAmountXrp() != null && project.getEscrowAmountXrp() > 0
                ? project.getEscrowAmountXrp()
                : 10L;

        String addressForCore = participantAddress != null ? participantAddress.trim() : "";
        if (addressForCore.isEmpty()) {
            log.error("[참여신청] participantAddress 누락/빈값 | projectId={} | rawLength={}",
                    projectId, participantAddress != null ? participantAddress.length() : 0);
            throw new IllegalArgumentException("participantAddress is required");
        }

        log.info("[참여신청] Core createEscrow 호출 | projectId={} | addressForCore={}... | amountXrp={}",
                projectId, addressForCore.substring(0, Math.min(12, addressForCore.length())) + "...", amountXrp);
        CoreClient.CoreEscrowCreateResponse coreResponse;
        try {
            coreResponse = coreClient.createEscrow(
                    String.valueOf(projectId),
                    addressForCore,
                    amountXrp
            );
        } catch (CoreClientException e) {
            log.error("[참여신청] Core createEscrow 실패 | projectId={} | addressForCore={}... | error={}",
                    projectId, addressForCore.substring(0, Math.min(12, addressForCore.length())) + "...", e.getMessage(), e);
            throw new IllegalStateException("블록체인 에스크로 생성 실패. Core 서버 확인: " + e.getMessage());
        }

        log.info("[참여신청] Core createEscrow 성공 | projectId={} | txHash={}", projectId, coreResponse.txHash());
        Participant participant = Participant.builder()
                .projectId(projectId)
                .participantAddress(addressForCore)
                .escrowOwnerAddress(coreResponse.ownerAddress())
                .offerSequence(coreResponse.offerSequence())
                .escrowTxHash(coreResponse.txHash())
                .active(true)
                .build();
        Participant saved = participantRepository.save(participant);
        log.info("[참여신청] DB 저장 완료 | projectId={} | participantId={}", projectId, saved.getId());
        return saved;
    }

    /**
     * 연구 참여 철회 — Core 에스크로 취소 후 DB 비활성화
     */
    @Transactional
    public Participant withdraw(Long projectId, String participantAddress) {
        log.info("[참여철회] withdraw 시작 | projectId={} | participantAddress={}...",
                projectId, participantAddress != null ? participantAddress.substring(0, Math.min(12, participantAddress.length())) + "..." : "null");
        Participant participant = participantRepository
                .findByProjectIdAndParticipantAddress(projectId, participantAddress)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (!participant.isActive()) {
            throw new IllegalStateException("Already withdrawn");
        }

        if (participant.getEscrowOwnerAddress() == null || participant.getOfferSequence() == null) {
            throw new IllegalStateException("Escrow info missing, cannot cancel");
        }

        try {
            log.info("[참여철회] Core cancelEscrow 호출 | participantId={} | ownerAddress={}... | offerSequence={}",
                    participant.getId(), participant.getEscrowOwnerAddress() != null ? participant.getEscrowOwnerAddress().substring(0, Math.min(12, participant.getEscrowOwnerAddress().length())) + "..." : "null",
                    participant.getOfferSequence());
            coreClient.cancelEscrow(participant.getEscrowOwnerAddress(), participant.getOfferSequence());
        } catch (CoreClientException e) {
            log.error("[참여철회] Core cancelEscrow 실패 | participantId={} | error={}", participant.getId(), e.getMessage(), e);
            throw new IllegalStateException("블록체인 에스크로 취소 실패: " + e.getMessage());
        }

        log.info("[참여철회] withdraw 성공 | participantId={}", participant.getId());
        participant.setActive(false);
        participant.setWithdrawnAt(java.time.Instant.now());
        return participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<Participant> listByProject(Long projectId) {
        return participantRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Participant getByProjectAndAddress(Long projectId, String participantAddress) {
        return participantRepository.findByProjectIdAndParticipantAddress(projectId, participantAddress)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));
    }
}
