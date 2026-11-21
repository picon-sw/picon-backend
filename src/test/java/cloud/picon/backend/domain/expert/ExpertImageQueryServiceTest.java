package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.code.ExpertErrorCode;
import cloud.picon.backend.domain.expert.dto.ExpertImageData;
import cloud.picon.backend.domain.expert.dto.ExpertImageResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertImageRepository;
import cloud.picon.backend.domain.expert.repository.ExpertImageView;
import cloud.picon.backend.domain.expert.service.ExpertImageQueryService;
import cloud.picon.backend.domain.expert.service.ExpertImageService;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpertImageQueryServiceTest {

    private final ExpertDetailRepository experts = mock(ExpertDetailRepository.class);
    private final ExpertImageRepository images = mock(ExpertImageRepository.class);
    private final ExpertImageQueryService service = new ExpertImageQueryService(experts, images);

    @Test
    void invisibleExpertIs404BeforeAttemptingHugeOffsetCount() {
        assertThatThrownBy(() -> service.getImages(1, null, Integer.MAX_VALUE, 100))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExpertErrorCode.EXPERT_NOT_FOUND));
        verifyNoInteractions(images);
    }

    @Test
    void hugeOffsetCountsOnlyTargetExpertAndFieldAndDoesNotAttemptJpaPage() {
        when(experts.existsVisibleExpert(1)).thenReturn(true);
        when(images.countExpertImages(1, "EDITING")).thenReturn(25L);

        var page = service.getImages(1, Specialty.EDITING, Integer.MAX_VALUE, 100);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getPageable().getOffset()).isEqualTo(214748364700L);
        assertThat(responseFor(page).last()).isTrue();
        verify(images).countExpertImages(1, "EDITING");
        verifyNoMoreInteractions(images);
    }

    @Test
    void offsetEqualToTotalIsEmptyAndPageCountDoesNotOverflow() {
        long offset = (long) Integer.MAX_VALUE * 10;
        when(experts.existsVisibleExpert(1)).thenReturn(true);
        when(images.countExpertImages(1, null)).thenReturn(offset);
        var page = service.getImages(1, null, Integer.MAX_VALUE, 10);
        assertThat(page.getContent()).isEmpty();
        assertThat(responseFor(page).totalPages()).isEqualTo(Integer.MAX_VALUE);
        assertThat(responseFor(page).last()).isTrue();
        verify(images).countExpertImages(1, null);
        verifyNoMoreInteractions(images);
    }

    @Test
    void realDataBeyondJpaLimitFailsExplicitlyInsteadOfReturningAnEmptyPage() {
        when(experts.existsVisibleExpert(1)).thenReturn(true);
        when(images.countExpertImages(1, null)).thenReturn((long) Integer.MAX_VALUE * 10 + 1);
        assertThatThrownBy(() -> service.getImages(1, null, Integer.MAX_VALUE, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessageContaining("offset");
        verify(images).countExpertImages(1, null);
        verifyNoMoreInteractions(images);
    }

    @Test
    void pageCountSupportsValuesAboveIntegerMaxWithoutUsingJpaIntPageCount() {
        var page = new PageImpl<ExpertImageData>(List.of(),
                PageRequest.of(0, 1), (long) Integer.MAX_VALUE + 1);
        var response = responseFor(page);
        assertThat(response.totalPages()).isEqualTo(2147483648L);
        assertThat(response.last()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN"})
    void invalidSpecialtyProjectionIsAnIntegrityError(String value) {
        when(experts.existsVisibleExpert(1)).thenReturn(true);
        var row = mock(ExpertImageView.class);
        when(row.getImageId()).thenReturn(101L);
        when(row.getSpecialty()).thenReturn(value);
        var pageable = PageRequest.of(0, 10);
        when(images.findExpertImages(1, null, pageable)).thenReturn(new PageImpl<>(List.of(row), pageable, 1));
        assertThatThrownBy(() -> service.getImages(1, null, 0, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    private static PageResponse<ExpertImageResponse> responseFor(Page<ExpertImageData> page) {
        var queryService = mock(ExpertImageQueryService.class);
        when(queryService.getImages(1, null, page.getNumber(), page.getSize())).thenReturn(page);
        var urls = mock(S3ImageUrlProvider.class);
        var response = new ExpertImageService(queryService, urls).getImages(1, null, page.getNumber(), page.getSize());
        verifyNoInteractions(urls);
        return response;
    }
}
