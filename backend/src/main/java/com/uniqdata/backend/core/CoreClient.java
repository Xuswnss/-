package com.uniqdata.backend.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Core (Nest.js 블록체인 서버) API 호출.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreClient {

    @Value("${core.base-url:http://localhost:3000/api}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * 에스크로 생성 (참여 신청 시 Core 호출)
     */
    public CoreEscrowCreateResponse createEscrow(String projectId, String participantAddress, long amountXrp) {
        String url = baseUrl + "/escrow";
        var body = new CoreEscrowCreateRequest(projectId, participantAddress, amountXrp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("[Core] createEscrow 요청 | url={} | projectId={} | participantAddress={}... | amountXrp={}",
                url, projectId, participantAddress != null ? participantAddress.substring(0, Math.min(12, participantAddress.length())) + "..." : "null", amountXrp);
        try {
            var response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    CoreEscrowCreateResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[Core] createEscrow 성공 | projectId={} | txHash={} | offerSequence={}",
                        projectId, response.getBody().txHash(), response.getBody().offerSequence());
                return response.getBody();
            }
            log.error("[Core] createEscrow 비정상 응답 | status={} | body={}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("[Core] createEscrow 실패 | url={} | projectId={} | participantAddress_len={} | error={} | cause={}",
                    url, projectId, participantAddress != null ? participantAddress.length() : 0, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "null", e);
            throw new CoreClientException("Core 에스크로 생성 실패: " + e.getMessage());
        }
        throw new CoreClientException("Core 에스크로 생성 실패");
    }

    /**
     * 에스크로 취소 (참여 철회 시 Core 호출)
     */
    public CoreEscrowCancelResponse cancelEscrow(String ownerAddress, long offerSequence) {
        String url = baseUrl + "/escrow/cancel";
        var body = new CoreEscrowCancelRequest(ownerAddress, offerSequence);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("[Core] cancelEscrow 요청 | url={} | ownerAddress={}... | offerSequence={}",
                url, ownerAddress != null ? ownerAddress.substring(0, Math.min(12, ownerAddress.length())) + "..." : "null", offerSequence);
        try {
            var response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    CoreEscrowCancelResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[Core] cancelEscrow 성공 | txHash={}", response.getBody().txHash());
                return response.getBody();
            }
            log.error("[Core] cancelEscrow 비정상 응답 | status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("[Core] cancelEscrow 실패 | url={} | ownerAddress={} | offerSequence={} | error={} | cause={}",
                    url, ownerAddress, offerSequence, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null", e);
            throw new CoreClientException("Core 에스크로 취소 실패: " + e.getMessage());
        }
        throw new CoreClientException("Core 에스크로 취소 실패");
    }

    /**
     * 에스크로 KPI (대시보드용)
     */
    public CoreSummaryResponse getSummary() {
        String url = baseUrl + "/summary";
        log.debug("[Core] getSummary 요청 | url={}", url);
        try {
            var response = restTemplate.getForEntity(url, CoreSummaryResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.error("[Core] getSummary 비정상 응답 | status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("[Core] getSummary 실패 | url={} | error={} | cause={}",
                    url, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null", e);
            throw new CoreClientException("Core summary 조회 실패: " + e.getMessage());
        }
        throw new CoreClientException("Core summary 조회 실패");
    }

    // --- DTOs (Core API 스펙에 맞춤) ---

    public record CoreEscrowCreateRequest(String projectId, String participantAddress, long amountXrp) {}

    public record CoreEscrowCreateResponse(
            String txHash,
            String escrowId,
            String ownerAddress,
            long offerSequence
    ) {}

    public record CoreEscrowCancelRequest(String ownerAddress, long offerSequence) {}

    public record CoreEscrowCancelResponse(String txHash) {}

    public record CoreSummaryResponse(
            String escrow_wallet_address,
            String escrow_balance_drops,
            double escrow_balance_xrp,
            String network
    ) {}
}
