package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.repository.ExpertImageRepository;
import cloud.picon.backend.domain.expert.service.PortfolioImageQueryService;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioImageQueryServiceTest {

    private final ExpertImageRepository repository = mock(ExpertImageRepository.class);
    private final PortfolioImageQueryService service = new PortfolioImageQueryService(repository);

    @Test
    void oversizedOffsetCountsOnceAndDoesNotRunUnsupportedPageQuery() {
        when(repository.countPortfolioImages("EDITING")).thenReturn(25L);

        var result = service.getPortfolioImages(Specialty.EDITING, Integer.MAX_VALUE, 100);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getPageable().getOffset()).isEqualTo(214748364700L);
        verify(repository).countPortfolioImages("EDITING");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void offsetEqualToTotalIsAnEmptyPage() {
        long offset = (long) Integer.MAX_VALUE * 10;
        when(repository.countPortfolioImages(null)).thenReturn(offset);

        var result = service.getPortfolioImages(null, Integer.MAX_VALUE, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(offset);
        verify(repository).countPortfolioImages(null);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void realDataBeyondJpaOffsetLimitFailsExplicitlyInsteadOfReturningFalseEmptyPage() {
        when(repository.countPortfolioImages(null)).thenReturn((long) Integer.MAX_VALUE * 10 + 1);

        assertThatThrownBy(() -> service.getPortfolioImages(null, Integer.MAX_VALUE, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessageContaining("offset");
        verify(repository).countPortfolioImages(null);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void ordinaryPageUsesExistingPageQueryWithoutAnExtraCount() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findPortfolioImages("SHOOTING", pageable)).thenReturn(Page.empty(pageable));

        assertThat(service.getPortfolioImages(Specialty.SHOOTING, 0, 10)).isEmpty();

        verify(repository).findPortfolioImages("SHOOTING", pageable);
        verifyNoMoreInteractions(repository);
    }
}
