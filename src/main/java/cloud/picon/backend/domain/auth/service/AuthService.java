package cloud.picon.backend.domain.auth.service;

import cloud.picon.backend.domain.auth.client.KakaoOAuthClient;
import cloud.picon.backend.domain.auth.client.KakaoProperties;
import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import cloud.picon.backend.domain.auth.dto.LoginResult;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResponse;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResult;
import cloud.picon.backend.domain.auth.repository.RefreshTokenRepository;
import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.domain.member.repository.MemberRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.jwt.JwtProperties;
import cloud.picon.backend.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoProperties kakaoProperties;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    // 카카오 API 호출이 있어 @Transactional을 붙이지 않음 (외부 호출 동안 DB 연결을 붙잡지 않도록)
    public LoginResult kakaoLogin(KakaoLoginRequest request) {
        validateRedirectUri(request.redirectUri());

        Long kakaoId = kakaoOAuthClient.getKakaoId(request.code(), request.redirectUri());

        Member member;
        boolean isNewMember = false;
        Optional<Member> foundMember = memberRepository.findByKakaoId(kakaoId);
        if (foundMember.isPresent()) {
            member = foundMember.get();
        } else {
            try {
                member = memberRepository.save(Member.createKakaoMember(kakaoId));
                isNewMember = true;
            } catch (DataIntegrityViolationException e) {
                // 동시에 처음 로그인해서 다른 요청이 먼저 저장한 경우, 그 회원을 사용
                member = memberRepository.findByKakaoId(kakaoId).orElseThrow(() -> e);
            }
        }

        Tokens tokens = issueTokens(member.getId());
        LoginResponse response = new LoginResponse(
                tokens.accessToken(),
                jwtProvider.getAccessTokenExpiresIn(),
                isNewMember,
                member.isOnboarded());
        return new LoginResult(response, tokens.refreshToken());
    }

    // 사용한 Refresh Token은 바로 삭제하고 새로 발급 (로테이션)
    public TokenRefreshResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = refreshTokenRepository.findMemberIdAndDelete(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Tokens tokens = issueTokens(memberId);
        TokenRefreshResponse response = new TokenRefreshResponse(
                tokens.accessToken(),
                jwtProvider.getAccessTokenExpiresIn());
        return new TokenRefreshResult(response, tokens.refreshToken());
    }

    private Tokens issueTokens(Long memberId) {
        String accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken();
        refreshTokenRepository.save(refreshToken, memberId, jwtProperties.refreshTokenExpiration());
        return new Tokens(accessToken, refreshToken);
    }

    private void validateRedirectUri(String redirectUri) {
        if (!kakaoProperties.allowedRedirectUris().contains(redirectUri)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}