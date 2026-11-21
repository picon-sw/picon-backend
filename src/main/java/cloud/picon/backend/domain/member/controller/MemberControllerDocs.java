package cloud.picon.backend.domain.member.controller;

import cloud.picon.backend.domain.member.dto.OnboardingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Members", description = "회원")
public interface MemberControllerDocs {

    @Operation(
            summary = "온보딩 완료",
            description = """
                    약관 동의부터 프로필 입력까지 한 번에 받아 온보딩을 완료합니다. 한 번만 할 수 있습니다.
                    moods: BRIGHT(밝음), NATURAL(자연스러움), CALM(차분함), SOPHISTICATED(세련됨),
                    DREAMY(몽환적), DISTINCTIVE(개성적), DRAMATIC(극적 표현) 중 1~3개
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "온보딩 완료"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 형식 오류, 필수 약관 미동의, 허용되지 않은 분위기 값",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "INVALID_REQUEST", "message": "요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "토큰 누락·만료·위조, 또는 회원이 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "UNAUTHORIZED", "message": "로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 온보딩을 완료한 회원",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "INVALID_STATUS", "message": "현재 상태에서는 처리할 수 없습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<Void>> completeOnboarding(
            @Parameter(hidden = true) Long memberId, OnboardingRequest request);
}