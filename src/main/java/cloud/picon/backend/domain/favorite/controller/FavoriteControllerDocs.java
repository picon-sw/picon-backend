package cloud.picon.backend.domain.favorite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface FavoriteControllerDocs {

    String SUCCESS = """
            {"isSuccess":true,"code":"OK","message":"요청에 성공했습니다."}
            """;
    String INVALID = """
            {"isSuccess":false,"code":"INVALID_REQUEST","message":"요청 값이 올바르지 않습니다."}
            """;
    String UNAUTHORIZED = """
            {"isSuccess":false,"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
            """;
    String INTERNAL_ERROR = """
            {"isSuccess":false,"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다."}
            """;

    @Operation(summary = "전문가 찜 추가", security = @SecurityRequirement(name = "bearerAuth"),
            description = "로그인한 회원의 찜에 전문가를 추가합니다. 본인은 찜할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 추가 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = SUCCESS))),
            @ApiResponse(responseCode = "400", description = "잘못된 전문가 ID 또는 본인 찜",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED))),
            @ApiResponse(responseCode = "404", description = "전문가가 없거나 노출 대상 아님",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"EXPERT_NOT_FOUND","message":"전문가 정보를 찾을 수 없습니다."}
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 찜한 전문가",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"ALREADY_FAVORITED","message":"이미 찜한 전문가입니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<Void>> add(
            @Parameter(description = "전문가 회원 ID") long expertId,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "전문가 찜 해제", security = @SecurityRequirement(name = "bearerAuth"),
            description = "로그인한 회원의 찜을 해제합니다. 전문가의 노출 여부와 관계없이 해제하며, 찜이 없어도 성공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 해제 성공 또는 해제할 찜 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = SUCCESS))),
            @ApiResponse(responseCode = "400", description = "잘못된 전문가 ID",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<Void>> remove(
            @Parameter(description = "전문가 회원 ID") long expertId,
            @Parameter(hidden = true) Long memberId);
}
