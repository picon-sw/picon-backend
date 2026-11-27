package cloud.picon.backend.domain.region.service;

import cloud.picon.backend.domain.region.dto.RegionResponse;
import cloud.picon.backend.domain.region.repository.RegionQueryRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private static final int MAX_KEYWORD_LENGTH = 20;

    private final RegionQueryRepository regionQueryRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> findRegions(String keyword) {
        String normalized = keyword == null ? "" : keyword.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return regionQueryRepository.findDistricts(normalized);
    }
}