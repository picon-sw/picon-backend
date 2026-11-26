package cloud.picon.backend.domain.review.controller;

import cloud.picon.backend.domain.review.dto.ReviewCreateRequest;
import cloud.picon.backend.domain.review.dto.ReviewResponse;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

public interface ReviewControllerDocs {

    String INVALID = """
            {"isSuccess":false,"code":"INVALID_REQUEST","message":"요청 값이 올바르지 않습니다."}
            """;
    String UNAUTHORIZED = """
            {"isSuccess":false,"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
            """;
    String INTERNAL = """
            {"isSuccess":false,"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다."}
            """;

    @Operation(summary = "리뷰 등록", security = @SecurityRequirement(name = "bearerAuth"),
            description = "완료된 예약의 의뢰 회원이 리뷰를 작성합니다. 별점은 1~5점입니다. "
                    + "본문은 선택이며 최대 2,000 Unicode 코드 포인트입니다. "
                    + "태그는 선택 JSON 배열이며 직렬화한 UTF-8 크기가 최대 16 KiB입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "리뷰 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "의뢰 회원 불일치",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"FORBIDDEN","message":"접근 권한이 없습니다."}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예약 정보 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"NOT_FOUND","message":"요청한 대상을 찾을 수 없습니다."}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "예약 미완료 또는 리뷰 중복",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "INVALID_STATUS", value = """
                                    {"isSuccess":false,"code":"INVALID_STATUS","message":"현재 상태에서는 처리할 수 없습니다."}
                                    """),
                            @ExampleObject(name = "REVIEW_ALREADY_EXISTS", value = """
                                    {"isSuccess":false,"code":"REVIEW_ALREADY_EXISTS","message":"이미 등록된 리뷰가 있습니다."}
                                    """)})),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INTERNAL)))
    })
    ResponseEntity<ApiResponse<ReviewResponse>> register(long reservationId, ReviewCreateRequest request,
                                                         @Parameter(hidden = true) Long memberId);

    @Operation(summary = "전문가 리뷰 목록", security = @SecurityRequirement(name = "bearerAuth"),
            description = "최신순으로 조회합니다. 페이지 번호는 0부터 시작하며 기본 크기는 20건입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "전문가 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"EXPERT_NOT_FOUND","message":"전문가 정보를 찾을 수 없습니다."}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INTERNAL)))
    })
    ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> findReviews(long expertId, int page, int size,
                                                                         @Parameter(hidden = true) Long memberId);
}
