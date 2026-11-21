package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.ExpertSpecialtyResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Slf4j
@Component
public class ExpertSpecialtyValidator {

    public void validate(long expertId, List<ExpertSpecialtyResponse> values) {
        var seen = EnumSet.noneOf(Specialty.class);
        for (var value : values) {
            if (!seen.add(value.name())) {
                log.error("전문가 분야 중복 발견: expertId={}, specialty={}", expertId, value.name());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
    }
}
