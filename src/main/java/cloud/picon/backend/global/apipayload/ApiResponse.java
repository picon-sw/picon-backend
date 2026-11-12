package cloud.picon.backend.global.apipayload;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import cloud.picon.backend.global.apipayload.code.BaseSuccessCode;
import cloud.picon.backend.global.apipayload.code.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// 모든 API 응답의 공통 형식. result가 null이면 JSON에서 생략
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess,
        String code,
        String message,
        T result
) {
    public static <T> ApiResponse<T> onSuccess(T result) {
        return onSuccess(SuccessCode.OK, result);
    }

    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, String message, T result) {
        return new ApiResponse<>(false, code.getCode(), message, result);
    }
}