package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class ExpertProfileImageValidator {

    private static final Pattern URL_PREFIX = Pattern.compile("^(?:[a-z][a-z0-9+.-]*://|//)", Pattern.CASE_INSENSITIVE);

    public void validate(long expertId, String imageKey) {
        if (imageKey != null && URL_PREFIX.matcher(imageKey.strip()).find()) {
            // URL에 포함될 수 있는 토큰 등은 로그에 남기지 않는다.
            log.error("S3 키가 아닌 프로필 이미지 저장값 발견: expertId={}", expertId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
