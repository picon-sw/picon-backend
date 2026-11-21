package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.code.ExpertErrorCode;
import cloud.picon.backend.domain.expert.dto.ExpertDetailData;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertSearchRepository;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertDetailQueryService {

    private final ExpertDetailRepository detailRepository;
    private final ExpertSearchRepository searchRepository;
    private final ExpertSpecialtyValidator specialtyValidator;

    @Transactional(readOnly = true)
    public ExpertDetailData getDetail(long expertId, long memberId) {
        var expert = detailRepository.findExpert(expertId, memberId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_NOT_FOUND));
        var ids = List.of(expertId);
        var specialties = List.copyOf(searchRepository.findSpecialties(ids).getOrDefault(expertId, List.of()));
        specialtyValidator.validate(expertId, specialties);
        var regions = List.copyOf(searchRepository.findRegions(ids).getOrDefault(expertId, List.of()));
        var styles = List.copyOf(detailRepository.findPhotoStyles(expertId));
        String summary = expert.reviewSummary();
        // Unicode 공백만 있는 요약은 null로 반환하고, 내용이 있으면 저장값을 그대로 유지한다.
        if (summary != null && summary.codePoints().allMatch(codePoint -> Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint) || codePoint == 0x85)) {
            summary = null;
        }
        return new ExpertDetailData(expert.id(), expert.nickname(), expert.profileImageKey(), expert.introduction(),
                expert.careerYears(), specialties, regions, styles, expert.rating(), expert.reviewCount(),
                summary, expert.favorite());
    }
}
