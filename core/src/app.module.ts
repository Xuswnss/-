import { Module } from '@nestjs/common';
import { XrplModule } from './xrpl/xrpl.module';
import { EscrowModule } from './escrow/escrow.module';

@Module({
  imports: [XrplModule, EscrowModule],
})
export class AppModule {}
