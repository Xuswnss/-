import { Injectable, Logger, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { Client } from 'xrpl';

@Injectable()
export class XrplService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(XrplService.name);
  private client: Client;

  async onModuleInit() {
    const wss = process.env.XRPL_WSS_URL || 'wss://s.altnet.rippletest.net:51233';
    this.logger.log(`[XRPL] 연결 시도 | wssUrl=${wss}`);
    try {
      this.client = new Client(wss);
      await this.client.connect();
      this.logger.log(`[XRPL] 연결 성공 | wssUrl=${wss}`);
    } catch (err) {
      this.logger.error(
        `[XRPL] 연결 실패 | wssUrl=${wss} | error=${err instanceof Error ? err.message : String(err)}`,
        err instanceof Error ? err.stack : undefined,
      );
      throw err;
    }
  }

  async onModuleDestroy() {
    if (this.client?.isConnected()) {
      await this.client.disconnect();
      this.logger.log('[XRPL] 연결 해제');
    }
  }

  getClient(): Client {
    return this.client;
  }

  async getBalance(address: string): Promise<string> {
    const resp = await this.client.request({
      command: 'account_info',
      account: address,
    });
    const balance = (resp.result as { account_data?: { Balance?: string } })?.account_data?.Balance;
    return balance || '0';
  }

  /**
   * 주소만 필요할 때 사용. ESCROW_WALLET_ADDRESS를 사용 (secret 사용 안 함)
   */
  async getEscrowWalletAddress(): Promise<string> {
    const address = process.env.ESCROW_WALLET_ADDRESS;
    if (!address || !address.trim()) {
      throw new Error('ESCROW_WALLET_ADDRESS is not set');
    }
    return address.trim();
  }

  async getEscrowWallet(): Promise<{ address: string; wallet: import('xrpl').Wallet }> {
    const secret = process.env.ESCROW_WALLET_SECRET;
    if (!secret) {
      this.logger.error('[XRPL] 에스크로 지갑 시드 없음 | reason=ESCROW_WALLET_SECRET_env_not_set');
      throw new Error('ESCROW_WALLET_SECRET is not set');
    }
    try {
      const { Wallet } = await import('xrpl');
      const wallet = Wallet.fromSeed(secret);
      this.logger.debug(`[XRPL] 에스크로 지갑 로드 | address=${wallet.address.slice(0, 12)}...`);
      return { address: wallet.address, wallet };
    } catch (err) {
      this.logger.error(
        `[XRPL] 에스크로 지갑 로드 실패 | error=${err instanceof Error ? err.message : String(err)}`,
        err instanceof Error ? err.stack : undefined,
      );
      throw err;
    }
  }
}
