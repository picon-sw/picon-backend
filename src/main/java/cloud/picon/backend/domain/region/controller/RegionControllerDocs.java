package cloud.picon.backend.domain.region.controller;

import cloud.picon.backend.domain.region.dto.RegionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Regions", description = "지역")
public interface RegionControllerDocs {

    @Operation(summary = "지역 목록 조회", security = @SecurityRequirement(name = "bearerAuth"), description = """
            시군구 단위 지역과 지역별 활동 중인 전문가 수를 조회합니다.
            읍면동은 포함하지 않고, 시군구가 없는 시도(세종 등)는 시도 행(secondDepth = null)으로 내려줍니다.
            keyword가 시도(firstDepth)나 시군구(secondDepth)에 포함되면 결과에 나옵니다. 앞뒤 공백은 지우며, 없거나 공백이면 전체 목록입니다.
            expertCount는 전문가 검색에 나오는 전문가(닉네임이 있는 전문가) 기준입니다.
            지역 코드 오름차순으로 정렬합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK: 조회 성공 (결과가 없으면 빈 배열)"),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: keyword가 20자를 넘음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INVALID_REQUEST", value = """
                            {"isSuccess":false,"code":"INVALID_REQUEST","message":"요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 인증 누락 또는 유효하지 않은 토큰",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "UNAUTHORIZED", value = """
                            {"isSuccess":false,"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: 서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                            {"isSuccess":false,"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<List<RegionResponse>>> getRegions(
            @Parameter(description = "검색어 (선택, 최대 20자)") String keyword);
}