package cloud.picon.backend.domain.request.dto;

import cloud.picon.backend.domain.request.entity.RequestImageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequestImageUploadUrlRequest(
        @Schema(description = "올릴 사진 목록 (참고 사진 최대 1장, 보정할 사진 최대 30장)")
        @NotNull @Size(min = 1, max = 31) List<@NotNull @Valid Image> images) {

    public record Image(
            @Schema(description = "REFERENCE(참고 사진) 또는 TO_EDIT(보정할 사진)")
            @NotNull RequestImageType imageType,
            @Schema(description = "image/jpeg, image/png, image/webp 중 하나")
            @NotNull String contentType,
            @Schema(description = "파일 크기(바이트), 최대 10MB(10,485,760)")
            @NotNull @Min(1) @Max(10_485_760) Long fileSize) {
    }
}