import { Body, Controller, Get, Post } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { EscrowService } from './escrow.service';
import {
  CreateEscrowDto,
  CancelEscrowDto,
  CreateEscrowResponseDto,
  CancelEscrowResponseDto,
  SummaryResponseDto,
} from './dto';

@Controller()
export class EscrowController {
  constructor(private readonly escrow: EscrowService) {}

  @Post('escrow')
  @ApiTags('Escrow')
  @ApiOperation({
    summary: '에스크로 생성 (연구 참여 신청)',
    description:
      '참여자가 연구에 신청할 때 호출. XRPL에 EscrowCreate 트랜잭션을 보내 참여자 주소로 지급될 XRP를 예치합니다. 응답의 ownerAddress, offerSequence 는 Backend에 저장해 두었다가 참여 철회 시 cancel 에 사용하세요.',
  })
  @ApiResponse({ status: 201, description: '에스크로 생성 성공', type: CreateEscrowResponseDto })
  @ApiResponse({ status: 400, description: '잘못된 요청 (예: projectId 범위 초과)' })
  @ApiResponse({ status: 500, description: 'XRPL 연결/서명 실패 (WSS URL, ESCROW_WALLET_SECRET 확인)' })
  async createEscrow(@Body() body: CreateEscrowDto & { participant_address?: string }) {
    const participantAddress =
      body.participantAddress ?? (body as { participant_address?: string }).participant_address ?? '';
    return this.escrow.createEscrow({
      projectId: body.projectId,
      participantAddress,
      amountXrp: body.amountXrp,
      finishAfterLedgerTime: body.finishAfterLedgerTime,
    });
  }

  @Post('escrow/cancel')
  @ApiTags('Escrow')
  @ApiOperation({
    summary: '에스크로 취소 (연구 참여 철회)',
    description:
      '참여자가 연구를 철회할 때 호출. createEscrow 시 반환된 ownerAddress, offerSequence 를 Body 로 보냅니다. (Backend DB에 저장해 둔 값 사용)',
  })
  @ApiResponse({ status: 201, description: '에스크로 취소 성공', type: CancelEscrowResponseDto })
  @ApiResponse({ status: 400, description: '권한 없음 (다른 지갑의 에스크로는 취소 불가)' })
  @ApiResponse({ status: 500, description: 'XRPL 제출 실패' })
  async cancelEscrow(@Body() body: CancelEscrowDto) {
    return this.escrow.cancelEscrow(body.ownerAddress, body.offerSequence);
  }

  @Get('summary')
  @ApiTags('Summary')
  @ApiOperation({
    summary: '에스크로 KPI (대시보드용)',
    description:
      '에스크로 전용 지갑 잔액 등. Wireframe 의 "에스크로 총액" 에 해당. total_participants, total_datapoints 는 연구 Backend DB에서 집계 후 프론트에서 합쳐서 표시하면 됩니다.',
  })
  @ApiResponse({ status: 200, description: 'KPI 조회 성공', type: SummaryResponseDto })
  async getSummary() {
    return this.escrow.getSummary();
  }
}
