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

        // 로그인할 때마다 새 세션 시작
        Tokens tokens = issueTokens(member.getId(), jwtProvider.createSessionId());
        LoginResponse response = new LoginResponse(
                tokens.accessToken(),
                jwtProvider.getAccessTokenExpiresIn(),
                isNewMember,
                member.isOnboarded());
        return new LoginResult(response, tokens.refreshToken());
    }

    // 사용한 Refresh Token은 바로 삭제하고 같은 세션으로 새로 발급 (로테이션)
    public TokenRefreshResult refresh(String refreshToken) {
        String sessionId = extractSessionId(refreshToken).orElseThrow(this::invalidRefreshToken);
        if (refreshTokenRepository.isSessionRevoked(sessionId)) {
            throw invalidRefreshToken();
        }
        Long memberId = refreshTokenRepository.findMemberIdAndDelete(refreshToken)
                .orElseThrow(this::invalidRefreshToken);
        if (!memberRepository.existsById(memberId)) {
            throw invalidRefreshToken();
        }

        Tokens tokens = issueTokens(memberId, sessionId);
        // 재발급하는 사이 로그아웃된 경우: 방금 저장한 토큰을 지우고 실패 처리
        if (refreshTokenRepository.isSessionRevoked(sessionId)) {
            refreshTokenRepository.delete(tokens.refreshToken());
            throw invalidRefreshToken();
        }

        TokenRefreshResponse response = new TokenRefreshResponse(
                tokens.accessToken(),
                jwtProvider.getAccessTokenExpiresIn());
        return new TokenRefreshResult(response, tokens.refreshToken());
    }

    // 세션 전체를 무효로 표시한 뒤 토큰 삭제. 쿠키가 없거나 이미 무효인 토큰이어도 성공 처리
    public void logout(String refreshToken) {
        extractSessionId(refreshToken).ifPresent(sessionId ->
                refreshTokenRepository.revokeSession(sessionId, jwtProperties.refreshTokenExpiration()));
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.delete(refreshToken);
        }
    }

    private Tokens issueTokens(Long memberId, String sessionId) {
        String accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken(sessionId);
        refreshTokenRepository.save(refreshToken, memberId, jwtProperties.refreshTokenExpiration());
        return new Tokens(accessToken, refreshToken);
    }

    private Optional<String> extractSessionId(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return Optional.empty();
        }
        return jwtProvider.extractSessionId(refreshToken);
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private void validateRedirectUri(String redirectUri) {
        if (!kakaoProperties.allowedRedirectUris().contains(redirectUri)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}