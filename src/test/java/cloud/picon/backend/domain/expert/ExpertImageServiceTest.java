package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.dto.ExpertImageData;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.service.ExpertImageQueryService;
import cloud.picon.backend.domain.expert.service.ExpertImageService;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpertImageServiceTest {

    @Test
    void nullKeyIsRejectedBeforeAnySigningEvenWhenAnotherRowIsValid() {
        // DB NOT NULL 제약과 별개로 조회 데이터의 null 키 방어를 검증한다.
        var query = mock(ExpertImageQueryService.class);
        var urls = mock(S3ImageUrlProvider.class);
        var service = new ExpertImageService(query, urls);
        var createdAt = LocalDateTime.of(2026, 11, 10, 14, 30);
        var rows = List.of(new ExpertImageData(1L, "portfolio/valid.jpg", Specialty.SHOOTING, createdAt),
                new ExpertImageData(2L, null, Specialty.EDITING, createdAt));
        when(query.getImages(1, null, 0, 10)).thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 2));
        assertThatThrownBy(() -> service.getImages(1, null, 0, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
        verifyNoInteractions(urls);
    }
}
