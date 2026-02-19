package com.uniqdata.backend.participant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "참여 철회 요청")
public class WithdrawRequest {

    @Schema(description = "참여자 XRPL 지갑 주소", example = "rN7n7otQDd6FczFgLdlqtyMVrn3e1DjxvV", required = true)
    private String participantAddress;
}
