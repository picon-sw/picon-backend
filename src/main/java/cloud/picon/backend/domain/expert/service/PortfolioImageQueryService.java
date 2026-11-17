package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.PortfolioImageData;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.repository.ExpertImageRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioImageQueryService {

    private final ExpertImageRepository expertImageRepository;

    @Transactional(readOnly = true)
    public Page<PortfolioImageData> getPortfolioImages(Specialty specialty, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String specialtyName = specialty == null ? null : specialty.name();
        // getOffset()은 long 곱셈을 사용한다. JPA에 넘길 수 없는 offset만 별도 count한다.
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            long total = expertImageRepository.countPortfolioImages(specialtyName);
            if (pageable.getOffset() >= total) {
                return new PageImpl<>(List.of(), pageable, total);
            }
            // 실제 데이터가 있는 페이지를 빈 결과로 위장하지 않는다.
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "현재 JPA 조회가 지원하는 offset 범위를 초과했습니다.");
        }
        return expertImageRepository.findPortfolioImages(specialtyName, pageable)
                .map(image -> new PortfolioImageData(image.getImageId(), image.getExpertId(),
                        image.getExpertNickname(), image.getImageKey(), Specialty.valueOf(image.getSpecialty())));
    }
}
