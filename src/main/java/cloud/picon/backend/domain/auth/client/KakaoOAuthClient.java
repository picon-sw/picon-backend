package cloud.picon.backend.domain.auth.client;

import cloud.picon.backend.domain.auth.client.dto.KakaoTokenResponse;
import cloud.picon.backend.domain.auth.client.dto.KakaoUserResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient kakaoRestClient;
    private final KakaoProperties kakaoProperties;

    public Long getKakaoId(String code, String redirectUri) {
        String kakaoAccessToken = requestAccessToken(code, redirectUri);
        return requestKakaoId(kakaoAccessToken);
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoProperties.clientId());
        form.add("client_secret", kakaoProperties.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        KakaoTokenResponse response = call(() -> kakaoRestClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class));
        return response.accessToken();
    }

    private Long requestKakaoId(String kakaoAccessToken) {
        KakaoUserResponse response = call(() -> kakaoRestClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class));
        return response.id();
    }

    private <T> T call(Supplier<T> request) {
        T body;
        try {
            body = request.get();
        } catch (HttpClientErrorException e) {
            log.warn("카카오 인증 실패: status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        } catch (RestClientException e) {
            log.error("카카오 서버 통신 실패", e);
            throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
        }
        if (body == null) {
            throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
        }
        return body;
    }
}