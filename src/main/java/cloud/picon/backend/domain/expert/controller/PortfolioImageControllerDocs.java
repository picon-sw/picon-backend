package cloud.picon.backend.domain.expert.controller;

import cloud.picon.backend.domain.expert.dto.PortfolioImagePageResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

@Tag(name = "Portfolio Images", description = "탐색 메인")
public interface PortfolioImageControllerDocs {

    @Operation(summary = "포트폴리오 이미지 목록 조회", description = """
            등록된 전문가 중 닉네임이 있는 회원의 이미지를 최신순(동일 시각은 이미지 ID 내림차순)으로 조회합니다.
            specialty 생략 시 촬영·편집 전체를 조회합니다. page는 0부터, size는 기본 10, 최대 100입니다.
            tag 파라미터는 지원하지 않으며 전달하면 400입니다.
            이미지 GET Presigned URL의 만료 시간은 600초로 설정합니다.
            IAM 자격 증명 만료 또는 정책 변경에 따라 실제 사용 가능 시간은 더 짧을 수 있습니다.
            범위 밖 페이지는 빈 목록을 반환합니다. 다만 offset이 JPA의 int 범위를 초과하고
            해당 페이지에 실제 데이터가 존재하면 현재 구현 한계로 500 INTERNAL_ERROR를 반환합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (결과가 없으면 빈 content)"),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: 잘못된 분야·페이지 또는 tag 전달"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 인증 누락 또는 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: 서버 오류")
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<PortfolioImagePageResponse>> getPortfolioImages(
            Specialty specialty, int page, int size,
            @Parameter(hidden = true) MultiValueMap<String, String> parameters);
}
