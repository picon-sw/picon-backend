package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.code.ExpertErrorCode;
import cloud.picon.backend.domain.expert.dto.ExpertImageData;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertImageRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertImageQueryService {

    private final ExpertDetailRepository expertRepository;
    private final ExpertImageRepository imageRepository;

    @Transactional(readOnly = true)
    public Page<ExpertImageData> getImages(long expertId, Specialty specialty, int page, int size) {
        if (!expertRepository.existsVisibleExpert(expertId)) {
            throw new BusinessException(ExpertErrorCode.EXPERT_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(page, size);
        String specialtyName = specialty == null ? null : specialty.name();
        // #17과 동일하게 JPA에 넘길 수 없는 offset만 count로 범위 밖인지 확인한다.
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            long total = imageRepository.countExpertImages(expertId, specialtyName);
            if (pageable.getOffset() >= total) {
                return new PageImpl<>(List.of(), pageable, total);
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "현재 JPA 조회가 지원하는 offset 범위를 초과했습니다.");
        }
        return imageRepository.findExpertImages(expertId, specialtyName, pageable).map(image ->
                new ExpertImageData(image.getImageId(), image.getImageKey(),
                        parseSpecialty(image.getImageId(), image.getSpecialty()),
                        LocalDateTime.parse(image.getCreatedAtText().strip().replace(' ', 'T'))));
    }

    private Specialty parseSpecialty(Long imageId, String value) {
        try {
            return Specialty.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("포트폴리오 이미지 분야 무결성 오류: imageId={}", imageId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
