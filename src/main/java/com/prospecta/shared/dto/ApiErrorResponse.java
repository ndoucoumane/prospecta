package com.prospecta.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private ErrorDetails error;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String message;
        private String traceId;
        private List<String> details;
    }

    public static ApiErrorResponse of(String code, String message, String traceId) {
        return ApiErrorResponse.builder()
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .traceId(traceId)
                        .build())
                .build();
    }

    public static ApiErrorResponse of(String code, String message, String traceId, List<String> details) {
        return ApiErrorResponse.builder()
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .traceId(traceId)
                        .details(details)
                        .build())
                .build();
    }
}
