import { Injectable, Logger } from '@nestjs/common';
import { XrplService } from '../xrpl/xrpl.service';
import { EscrowCreate, EscrowCancel } from 'xrpl';
import { isValidClassicAddress, isValidXAddress } from 'ripple-address-codec';

/** 1 XRP = 1,000,000 drops */
const XRP_TO_DROPS = 1_000_000;

function isValidXrpAddress(addr: unknown): addr is string {
  if (typeof addr !== 'string') return false;
  const trimmed = addr.trim();
  return trimmed.length > 0 && (isValidClassicAddress(trimmed) || isValidXAddress(trimmed));
}

@Injectable()
export class EscrowService {
  private readonly logger = new Logger(EscrowService.name);

  constructor(private readonly xrpl: XrplService) {}

  /**
   * 연구 참여 시 에스크로 생성 (Backend에서 호출)
   * @param projectId 연구 ID (DestinationTag로 사용)
   * @param participantAddress 참여자 XRPL 주소 (에스크로 수령자)
   * @param amountXrp 예치할 XRP 수
   * @param finishAfterLedgerTime 연구 종료 시점 (Ripple epoch 초). 없으면 기본 30일 후
   */
  async createEscrow(params: {
    projectId: string;
    participantAddress: string;
    amountXrp: number;
    finishAfterLedgerTime?: number;
  }): Promise<{ txHash: string; escrowId: string; ownerAddress: string; offerSequence: number }> {
    const raw =
      typeof params.participantAddress === 'string' ? params.participantAddress : String(params.participantAddress ?? '');
    let participantAddress = raw.trim();
    // participantAddress가 없거나 유효하지 않으면 .env ESCROW_WALLET_ADDRESS 또는 지갑 주소 사용
    if (!participantAddress || !isValidXrpAddress(participantAddress)) {
      const envAddress = process.env.ESCROW_WALLET_ADDRESS?.trim();
      let fallbackAddress = envAddress && isValidXrpAddress(envAddress) ? envAddress : null;
      if (!fallbackAddress) {
        try {
          const { address } = await this.xrpl.getEscrowWallet();
          fallbackAddress = isValidXrpAddress(address) ? address : null;
        } catch {
          /* ignore */
        }
      }
      if (fallbackAddress) {
        this.logger.log(
          `[createEscrow] participantAddress 없음/무효 → fallback 사용 | projectId=${params.projectId} | address=${fallbackAddress.slice(0, 12)}...`,
        );
        participantAddress = fallbackAddress;
      } else {
        this.logger.error(
          `[createEscrow] 검증 실패 | reason=${participantAddress ? 'invalid_xrp_address_format' : 'participantAddress_empty_or_missing'} | projectId=${params.projectId} | receivedType=${typeof params.participantAddress} | receivedLength=${raw?.length ?? 0} | receivedPreview=${raw ? raw.slice(0, 12) + '...' : '(empty)'}`,
        );
        throw new Error(
          participantAddress
            ? 'participantAddress must be a valid XRP address (classic r... or X-address). Example: rN7n7otQDd6FczFgLdlqtyMVrn3e1DjxvV'
            : 'participantAddress is required and must be a non-empty XRP address (received empty or missing). Set ESCROW_WALLET_ADDRESS in .env for dev/test fallback.',
        );
      }
    }

    this.logger.log(
      `[createEscrow] 시작 | projectId=${params.projectId} | participantAddress=${participantAddress.slice(0, 8)}...${participantAddress.slice(-4)} | amountXrp=${params.amountXrp}`,
    );

    const client = this.xrpl.getClient();
    const { address, wallet } = await this.xrpl.getEscrowWallet();

    const now = Math.floor(Date.now() / 1000);
    const rippleEpoch = now - 946684800; // 2000-01-01 00:00:00 UTC
    const finishAfter = params.finishAfterLedgerTime ?? rippleEpoch + 30 * 24 * 3600; // 기본 30일 후
    const cancelAfter = finishAfter + 7 * 24 * 3600; // 완료 후 7일 지나면 취소 가능(반환)

    const amountDrops = String(Math.round(params.amountXrp * XRP_TO_DROPS));
    const destinationTag = parseInt(params.projectId, 10) || 0;
    if (destinationTag > 0xFFFFFFFF || destinationTag < 0) {
      throw new Error('projectId must fit in DestinationTag (uint32)');
    }

    const tx: EscrowCreate = {
      TransactionType: 'EscrowCreate',
      Account: address,
      Destination: participantAddress,
      Amount: amountDrops,
      FinishAfter: finishAfter,
      CancelAfter: cancelAfter,
      ...(destinationTag > 0 ? { DestinationTag: destinationTag } : {}),
    };

    try {
      const prepared = await client.autofill(tx);
      const offerSequence = (prepared as { Sequence?: number }).Sequence;
      if (offerSequence == null) throw new Error('Missing Sequence in EscrowCreate');
      const signed = wallet.sign(prepared);
      const result = await client.submitAndWait(signed.tx_blob);
      const txHash = (result.result as { hash?: string }).hash ?? signed.hash;
      this.logger.log(
        `[createEscrow] 성공 | projectId=${params.projectId} | txHash=${txHash} | offerSequence=${offerSequence}`,
      );
      return {
        txHash,
        escrowId: txHash,
        ownerAddress: address,
        offerSequence,
      };
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : String(err);
      const errName = err instanceof Error ? err.constructor.name : typeof err;
      this.logger.error(
        `[createEscrow] XRPL 호출 실패 | projectId=${params.projectId} | participantAddress=${participantAddress.slice(0, 12)}... | amountXrp=${params.amountXrp} | error=${errName}: ${errMsg}`,
        err instanceof Error ? err.stack : undefined,
      );
      throw err;
    }
  }

  /**
   * 연구 참여 철회 시 에스크로 취소 (createEscrow 시 반환된 ownerAddress, offerSequence 사용)
   */
  async cancelEscrow(ownerAddress: string, offerSequence: number): Promise<{ txHash: string }> {
    this.logger.log(
      `[cancelEscrow] 시작 | ownerAddress=${ownerAddress ? ownerAddress.slice(0, 12) + '...' : '(empty)'} | offerSequence=${offerSequence}`,
    );
    const client = this.xrpl.getClient();
    const { address, wallet } = await this.xrpl.getEscrowWallet();
    if (address !== ownerAddress) {
      this.logger.error(
        `[cancelEscrow] 권한 없음 | reason=owner_address_mismatch | expectedOwner=${address.slice(0, 12)}... | receivedOwner=${ownerAddress ? ownerAddress.slice(0, 12) + '...' : '(empty)'}`,
      );
      throw new Error('Only the escrow owner (Core wallet) can cancel this escrow');
    }

    const tx: EscrowCancel = {
      TransactionType: 'EscrowCancel',
      Account: ownerAddress,
      Owner: ownerAddress,
      OfferSequence: offerSequence,
    };

    try {
      const prepared = await client.autofill(tx);
      const signed = wallet.sign(prepared);
      const result = await client.submitAndWait(signed.tx_blob);
      const txHash = (result.result as { hash?: string }).hash ?? signed.hash;
      this.logger.log(
        `[cancelEscrow] 성공 | txHash=${txHash} | offerSequence=${offerSequence}`,
      );
      return { txHash };
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : String(err);
      const errName = err instanceof Error ? err.constructor.name : typeof err;
      this.logger.error(
        `[cancelEscrow] XRPL 호출 실패 | ownerAddress=${ownerAddress ? ownerAddress.slice(0, 12) + '...' : '(empty)'} | offerSequence=${offerSequence} | error=${errName}: ${errMsg}`,
        err instanceof Error ? err.stack : undefined,
      );
      throw err;
    }
  }

  /**
   * 대시보드 KPI: 에스크로 총액 등 (Wireframe의 GET /api/v2/dashboard/summary 대응)
   */
  async getSummary(): Promise<{
    escrow_wallet_address: string;
    escrow_balance_drops: string;
    escrow_balance_xrp: number;
    network: string;
  }> {
    const address = await this.xrpl.getEscrowWalletAddress();
    const balanceDrops = await this.xrpl.getBalance(address);
    const balanceXrp = parseInt(balanceDrops, 10) / XRP_TO_DROPS;
    const network = process.env.XRPL_NETWORK || 'testnet';
    return {
      escrow_wallet_address: address,
      escrow_balance_drops: balanceDrops,
      escrow_balance_xrp: balanceXrp,
      network,
    };
  }
}
