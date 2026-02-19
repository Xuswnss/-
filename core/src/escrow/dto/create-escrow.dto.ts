import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/**
 * 연구 참여 시 에스크로 생성 요청
 * (연구 Backend → Core 블록체인 서버)
 */
export class CreateEscrowDto {
  @ApiProperty({
    description: '연구(프로젝트) ID. XRPL DestinationTag로 사용되며, uint32 범위 권장',
    example: '1',
  })
  projectId: string;

  @ApiProperty({
    description: '참여자의 XRPL 지갑 주소 (에스크로 수령자). 연구 완료 시 이 주소로 XRP 지급',
    example: 'rN7n7otQDd6FczFgLdlqtyMVrn3e1DjxvV',
  })
  participantAddress: string;

  @ApiProperty({
    description: '예치할 XRP 수량 (테스트넷에서는 Faucet으로 받은 테스트 XRP 사용)',
    example: 10,
    minimum: 0.000001,
  })
  amountXrp: number;

  @ApiPropertyOptional({
    description:
      '에스크로 만료 시점 (Ripple epoch 초). 생략 시 생성일 기준 30일 후. 연구 종료일 맞출 때 사용',
    example: 4102444800,
  })
  finishAfterLedgerTime?: number;
}
