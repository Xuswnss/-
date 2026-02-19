package com.uniqdata.backend.dashboard;

import com.uniqdata.backend.core.CoreClient;
import com.uniqdata.backend.core.CoreClientException;
import com.uniqdata.backend.project.ProjectRepository;
import com.uniqdata.backend.participant.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 대시보드 KPI. DB 집계 + Core(블록체인) 에스크로 잔액.
 * Wireframe: GET /api/v2/dashboard/summary
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final ParticipantRepository participantRepository;
    private final CoreClient coreClient;

    /**
     * total_projects, total_participants 는 DB 집계.
     * escrow_balance 는 Core에서 조회 (실패 시 0 또는 null 처리).
     */
    public Map<String, Object> getSummary() {
        long totalProjects = projectRepository.count();
        long totalParticipants = participantRepository.countByActive(true);

        double escrowBalanceXrp = 0;
        String escrowWalletAddress = null;
        String network = null;
        try {
            var coreSummary = coreClient.getSummary();
            escrowBalanceXrp = coreSummary.escrow_balance_xrp();
            escrowWalletAddress = coreSummary.escrow_wallet_address();
            network = coreSummary.network();
        } catch (CoreClientException e) {
            log.warn("Core summary unavailable: {}", e.getMessage());
        }

        return Map.of(
                "total_projects", totalProjects,
                "total_participants", totalParticipants,
                "total_datapoints", 0L,
                "escrow_balance", escrowBalanceXrp,
                "escrow_wallet_address", escrowWalletAddress != null ? escrowWalletAddress : "",
                "network", network != null ? network : "unknown"
        );
    }
}
