import { ApiProperty } from '@nestjs/swagger';

/** 에스크로 생성 성공 시 응답. Backend는 ownerAddress, offerSequence 를 참여자별로 저장해 두었다가 철회 시 cancel 에 사용 */
export class CreateEscrowResponseDto {
  @ApiProperty({ description: 'XRPL 트랜잭션 해시 (Explorer에서 조회 가능)' })
  txHash: string;

  @ApiProperty({ description: '에스크로 식별자 (현재는 txHash와 동일)' })
  escrowId: string;

  @ApiProperty({
    description: '에스크로 소유 주소. 취소 시 cancelEscrow 의 ownerAddress 로 전달',
  })
  ownerAddress: string;

  @ApiProperty({
    description: '에스크로 생성 시퀀스. 취소 시 cancelEscrow 의 offerSequence 로 전달',
  })
  offerSequence: number;
}

/** 에스크로 취소 성공 시 응답 */
export class CancelEscrowResponseDto {
  @ApiProperty({ description: 'EscrowCancel 트랜잭션 해시' })
  txHash: string;
}

/** 대시보드 KPI - 에스크로 지갑 잔액 등 (Wireframe 에스크로 총액 대응) */
export class SummaryResponseDto {
  @ApiProperty({ description: 'Core 에스크로 전용 지갑 주소' })
  escrow_wallet_address: string;

  @ApiProperty({ description: '잔액 (drops). 1 XRP = 1,000,000 drops' })
  escrow_balance_drops: string;

  @ApiProperty({ description: '잔액 (XRP 단위)' })
  escrow_balance_xrp: number;

  @ApiProperty({ description: '연결된 네트워크 (testnet / devnet / mainnet)' })
  network: string;
}
