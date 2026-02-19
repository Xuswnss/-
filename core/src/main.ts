import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { HttpExceptionFilter } from './common/filters/HttpExceptionFilter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.useGlobalFilters(new HttpExceptionFilter());
  const port = process.env.PORT ?? 3000;
  app.setGlobalPrefix('api');

  // Swagger: 백엔드 코어(블록체인) 전용 문서
  const config = new DocumentBuilder()
    .setTitle('Core API (Blockchain Server)')
    .setDescription(
      [
        '**연구 Backend(Java)** 가 호출하는 **블록체인 전용 API**입니다.',
        '',
        '- 연구 **참여 신청** 시 → 에스크로 생성 (XRPL)',
        '- 연구 **참여 철회** 시 → 에스크로 취소',
        '- 대시보드 **에스크로 총액** 등 KPI 조회',
        '',
        '테스트넷: XRPL Testnet 사용 (API 키 불필요, Faucet으로 테스트 XRP 발급)',
      ].join('\n'),
    )
    .setVersion('1.0')
    .addTag('Escrow', '연구 참여/철회 시 XRPL 에스크로 생성·취소')
    .addTag('Summary', '에스크로 지갑 잔액 등 대시보드 KPI')
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document, {
    customSiteTitle: 'Core API - 블록체인 서버',
    customfavIcon: undefined,
  });

  await app.listen(port);
  console.log(`Core (Blockchain Server) http://localhost:${port}/api`);
  console.log(`Swagger 문서 http://localhost:${port}/api/docs`);
}
bootstrap();
