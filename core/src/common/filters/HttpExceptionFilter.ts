import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';

/**
 * 전역 예외 필터. 모든 에러를 상세히 로그하고 HTTP 응답 반환.
 */
@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    const status =
      exception instanceof HttpException
        ? exception.getStatus()
        : HttpStatus.INTERNAL_SERVER_ERROR;

    const message =
      exception instanceof Error ? exception.message : 'Unknown error';

    const body: Record<string, unknown> = { statusCode: status, message };

    // 요청 정보
    const reqInfo = {
      method: request.method,
      url: request.url,
      path: request.path,
      query: request.query,
      bodyKeys: request.body ? Object.keys(request.body) : [],
      bodyPreview:
        request.body && typeof request.body === 'object'
          ? JSON.stringify(request.body).slice(0, 500)
          : undefined,
    };

    // 에러 로그 (상세)
    const exceptionName = exception instanceof Error ? exception.constructor.name : typeof exception;
    const stackTrace = exception instanceof Error ? exception.stack : undefined;
    this.logger.error(
      `[ERROR] exception=${exceptionName} | status=${status} | method=${request.method} | url=${request.url} | path=${request.path} | message=${message} | query=${JSON.stringify(request.query)} | bodyKeys=${reqInfo.bodyKeys.join(',') || 'none'} | bodyPreview=${reqInfo.bodyPreview ?? 'none'} | ip=${request.ip ?? 'unknown'} | userAgent=${(request.get?.('user-agent') ?? 'unknown').slice(0, 80)}`,
      stackTrace,
    );

    response.status(status).json(body);
  }
}
