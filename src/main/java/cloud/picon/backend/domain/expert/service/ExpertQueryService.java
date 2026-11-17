package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.domain.expert.repository.ExpertSearchRepository;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertQueryService {

    private final ExpertSearchRepository repository;

    @Transactional(readOnly = true)
    public ExpertSearchData search(ExpertSearchCondition condition, long memberId) {
        long total = repository.count(condition);
        if (condition.offset() >= total) {
            return new ExpertSearchData(List.of(), condition.page(), condition.size(), total);
        }
        var rows = repository.findPage(condition, memberId);
        if (rows.isEmpty()) {
            return new ExpertSearchData(List.of(), condition.page(), condition.size(), total);
        }
        var ids = rows.stream().map(ExpertSearchRepository.ExpertRow::id).toList();
        var specialties = repository.findSpecialties(ids);
        specialties.forEach((expertId, values) -> {
            var seen = EnumSet.noneOf(Specialty.class);
            for (var value : values) {
                if (!seen.add(value.name())) {
                    log.error("전문가 분야 중복 발견: expertId={}, specialty={}", expertId, value.name());
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
        });
        var regions = repository.findRegions(ids);
        var images = repository.findRepresentativeImages(ids, condition.specialty());
        var content = rows.stream().map(row -> new ExpertData(row.id(), row.nickname(), row.profileImage(),
                images.get(row.id()), row.introduction(), List.copyOf(specialties.getOrDefault(row.id(), List.of())),
                List.copyOf(regions.getOrDefault(row.id(), List.of())), row.careerYears(), row.rating(),
                row.reviewCount(), row.favorite())).toList();
        return new ExpertSearchData(content, condition.page(), condition.size(), total);
    }
}
