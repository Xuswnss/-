package com.uniqdata.backend.participant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Participants", description = "참여 신청·철회 (내부에서 Core 에스크로 생성/취소 호출)")
@RestController
@RequestMapping("/api/v2/projects/{projectId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "참여 신청", description = "Body에 participantAddress만 넣으세요. Core 에스크로 생성 후 참여자 저장.")
    @PostMapping("/enroll")
    public ResponseEntity<Participant> enroll(
            @PathVariable Long projectId,
            @RequestBody EnrollRequest body) {
        String participantAddress = body.getParticipantAddress();
        if (participantAddress == null || participantAddress.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(participantService.enroll(projectId, participantAddress));
    }

    @Operation(summary = "참여 철회", description = "Core 에스크로 취소 후 비활성화")
    @PostMapping("/withdraw")
    public ResponseEntity<Participant> withdraw(
            @PathVariable Long projectId,
            @RequestBody WithdrawRequest body) {
        String participantAddress = body.getParticipantAddress();
        if (participantAddress == null || participantAddress.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(participantService.withdraw(projectId, participantAddress));
    }

    @Operation(summary = "참여자 목록")
    @GetMapping
    public ResponseEntity<List<Participant>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(participantService.listByProject(projectId));
    }

    @Operation(summary = "참여자 1명 조회 (주소 기준)")
    @GetMapping("/by-address")
    public ResponseEntity<Participant> getByAddress(
            @PathVariable Long projectId,
            @RequestParam String participantAddress) {
        return ResponseEntity.ok(participantService.getByProjectAndAddress(projectId, participantAddress));
    }
}
