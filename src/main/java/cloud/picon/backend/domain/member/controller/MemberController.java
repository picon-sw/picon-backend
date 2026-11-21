package cloud.picon.backend.domain.member.controller;

import cloud.picon.backend.domain.member.dto.OnboardingRequest;
import cloud.picon.backend.domain.member.service.MemberService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.jwt.LoginMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

    private final MemberService memberService;

    @Override
    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(
            @LoginMemberId Long memberId,
            @Valid @RequestBody OnboardingRequest request) {
        memberService.completeOnboarding(memberId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}