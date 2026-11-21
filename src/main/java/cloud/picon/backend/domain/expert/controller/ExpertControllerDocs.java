package cloud.picon.backend.domain.expert.controller;

import cloud.picon.backend.domain.expert.dto.ExpertDetailResponse;
import cloud.picon.backend.domain.expert.dto.ExpertResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.apipayload.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Experts", description = "전문가 검색")
public interface ExpertControllerDocs {

    @Operation(summary = "전문가 검색 및 목록 조회", description = """
            등록 전문가 중 닉네임이 있는 회원을 조회합니다. 온보딩 완료 여부로 제한하지 않습니다.
            keyword는 앞뒤 공백을 제거한 뒤 닉네임·소개·전문가 사진 스타일 이름에서 부분 검색합니다.
            %, _는 일반 문자이며 서로 다른 필터는 AND 조건입니다.
            specialty와 최소·최대 가격은 같은 분야 행에 적용합니다. 분야 미지정 시 한 분야라도 가격 범위를 충족하면 포함합니다.
            지역·사진 스타일은 정확한 ID로 일치하며, 없는 ID는 빈 결과입니다.
            응답에는 전체 분야·가격과 전체 활동 지역을 반환합니다. 동일 분야 중복 데이터는 INTERNAL_ERROR입니다.
            sort는 rating(기본: 평점·리뷰 수·전문가 ID 내림차순), latest(전문가 등록일·ID 내림차순)입니다.
            page는 0부터, size는 기본 10·최대 100입니다. 범위 밖 페이지는 200과 빈 content입니다.
            대표 이미지는 선택 분야 최신 이미지를 우선하며 없으면 전체 최신 이미지로 대체합니다.
            이미지 생성 시각이 같으면 이미지 ID 내림차순입니다. 이미지가 없으면 null입니다.
            profile_image는 S3 키만 허용하고 null·공백이면 null입니다. 완성 URL 저장값은 서명하지 않고 INTERNAL_ERROR로 처리합니다.
            이미지 GET Presigned URL 만료 설정은 600초이며 자격 증명 만료·정책 변경에 따라 더 짧을 수 있습니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK: 전문가 페이지 조회 성공"),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: 잘못된 검색·가격·경력·정렬·페이지 값",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INVALID_REQUEST", value = """
                            {"isSuccess": false, "code": "INVALID_REQUEST", "message": "요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 누락·만료·위조 또는 잘못된 회원 ID",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "UNAUTHORIZED", value = """
                            {"isSuccess": false, "code": "UNAUTHORIZED", "message": "로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: 데이터 무결성·이미지 설정 또는 서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                            {"isSuccess": false, "code": "INTERNAL_ERROR", "message": "서버 오류가 발생했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<PageResponse<ExpertResponse>>> search(
            String keyword, Specialty specialty, Long photoStyleId, Long regionCode,
            Integer minPrice, Integer maxPrice, Integer minCareerYears, String sort, int page, int size,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "전문가 상세 정보 조회", description = """
            등록 전문가 중 닉네임이 설정된 회원의 상세 정보를 조회합니다. 온보딩 완료 여부와 관계없이 조회할 수 있습니다.
            specialties는 분야와 기본 가격, regions는 활동 지역, photoStyles는 ID 오름차순의 사진 스타일을 담습니다.
            관계 데이터가 없으면 해당 필드는 빈 배열입니다.
            profileImage는 만료될 수 있는 이미지 조회 URL이며, 이미지가 없으면 null입니다.
            rating과 reviewCount는 평점과 리뷰 수입니다. reviewSummary는 저장된 AI 리뷰 요약이며, 비어 있으면 null입니다.
            isFavorite는 로그인한 회원이 해당 전문가를 찜했는지 나타냅니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK: 전문가 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: 양수가 아니거나 Long으로 해석할 수 없는 ID",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INVALID_REQUEST", value = """
                            {"isSuccess": false, "code": "INVALID_REQUEST", "message": "요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 인증 실패 또는 잘못된 JWT 회원 ID",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "UNAUTHORIZED", value = """
                            {"isSuccess": false, "code": "UNAUTHORIZED", "message": "로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "404", description = "EXPERT_NOT_FOUND: 미등록 전문가 또는 닉네임이 비어 있는 전문가",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "EXPERT_NOT_FOUND", value = """
                            {"isSuccess": false, "code": "EXPERT_NOT_FOUND", "message": "전문가 정보를 찾을 수 없습니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: 중복 분야 데이터 또는 서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                            {"isSuccess": false, "code": "INTERNAL_ERROR", "message": "서버 오류가 발생했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<ExpertDetailResponse>> getDetail(
            @Parameter(description = "전문가 회원 ID", required = true) Long expertId,
            @Parameter(hidden = true) Long memberId);
}
