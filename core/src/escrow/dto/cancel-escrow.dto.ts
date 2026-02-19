import { ApiProperty } from '@nestjs/swagger';

/**
 * 연구 참여 철회 시 에스크로 취소 요청
 * createEscrow 응답으로 받은 ownerAddress, offerSequence 를 그대로 전달
 */
export class CancelEscrowDto {
  @ApiProperty({
    description: '에스크로를 생성한 지갑 주소 (Core 에스크로 지갑). createEscrow 응답의 ownerAddress',
    example: 'rHb9CJAWyB4rj91VRWn96DkukG4b8tyKjV',
  })
  ownerAddress: string;

  @ApiProperty({
    description: '에스크로 생성 트랜잭션의 Sequence. createEscrow 응답의 offerSequence (Backend DB에 저장해 두었다가 사용)',
    example: 12345,
  })
  offerSequence: number;
}
