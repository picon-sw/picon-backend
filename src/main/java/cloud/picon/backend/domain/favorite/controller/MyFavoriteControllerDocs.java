package cloud.picon.backend.domain.favorite.controller;

import cloud.picon.backend.domain.favorite.dto.MyFavoriteResponse;
import cloud.picon.backend.global.apipayload.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface MyFavoriteControllerDocs {

    @Operation(summary = "내 찜 목록 조회", security = @SecurityRequirement(name = "bearerAuth"),
            description = "로그인한 회원이 찜한 노출 대상 전문가를 최근 찜한 순으로 조회합니다. "
                    + "시작가는 등록된 촬영·편집 분야의 기본가 중 최솟값입니다. "
                    + "조회할 항목이 없으면 빈 content를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 번호 또는 크기",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"INVALID_REQUEST","message":"요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<PageResponse<MyFavoriteResponse>>> find(
            @Parameter(description = "0부터 시작하는 페이지 번호") int page,
            @Parameter(description = "페이지 크기 (1~100)") int size,
            @Parameter(hidden = true) Long memberId);
}
