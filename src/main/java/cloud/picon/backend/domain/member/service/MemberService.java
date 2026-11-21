package cloud.picon.backend.domain.member.service;

import cloud.picon.backend.domain.member.dto.OnboardingRequest;
import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.domain.member.entity.MemberMood;
import cloud.picon.backend.domain.member.repository.MemberMoodRepository;
import cloud.picon.backend.domain.member.repository.MemberRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMoodRepository memberMoodRepository;

    @Transactional
    public void completeOnboarding(Long memberId, OnboardingRequest request) {
        // 토큰은 유효하지만 회원이 없으면(탈퇴 등) 다시 로그인하도록 401
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (member.isOnboarded()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        member.completeOnboarding(request.nickname().strip(), request.birth(), request.gender(),
                request.phoneNumber(), request.serviceType(), request.marketingAgreed(), LocalDateTime.now());
        memberMoodRepository.saveAll(request.moods().stream()
                .map(mood -> new MemberMood(member, mood))
                .toList());
    }
}