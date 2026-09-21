package com.ilog.ilog.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드. `ErrorCode` 에 정해 둔 값과 같다.", example = "400")
        int status,

        @Schema(description = "에러 코드 이름. 프론트는 이 값으로 분기한다.", example = "INVALID_INPUT")
        String code,

        @Schema(description = "사용자에게 보여 줄 안내 문구. `ErrorCode` 의 메시지를 그대로 담는다.",
                example = "입력값이 올바르지 않습니다.")
        String message,

        @Schema(description = "입력값 검증에 실패한 항목 목록. 검증 오류가 아니면 빈 배열이다.")
        List<FieldError> errors,

        @Schema(description = "에러가 난 서버 시각 (KST)", example = "2026-09-21T09:30:00")
        LocalDateTime timestamp
) {
    public record FieldError(
            @Schema(description = "규칙을 어긴 요청 항목 이름. 본문 필드명 또는 쿼리 파라미터·경로 변수 이름이 들어간다.",
                    example = "title")
            String field,

            @Schema(description = "어떤 규칙을 어겼는지 알려 주는 문구", example = "제목은 필수입니다.")
            String reason
    ) {}

    public static ErrorResponse of(ErrorCode ec) {
        return of(ec, List.of());
    }

    public static ErrorResponse of(ErrorCode ec, List<FieldError> errors) {
        return new ErrorResponse(ec.getStatus().value(), ec.name(),
                ec.getMessage(), errors, LocalDateTime.now());
    }
}
