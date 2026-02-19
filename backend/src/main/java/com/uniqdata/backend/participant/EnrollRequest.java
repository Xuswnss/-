package com.uniqdata.backend.participant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "참여 신청 요청. participantAddress만 넣으면 됩니다.")
public class EnrollRequest {

    @Schema(description = "참여자 XRPL 지갑 주소 (r로 시작)", example = "rN7n7otQDd6FczFgLdlqtyMVrn3e1DjxvV", required = true)
    private String participantAddress;
}
