package cloud.picon.backend.domain.favorite;

import cloud.picon.backend.domain.expert.entity.Expert;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertRepository;
import cloud.picon.backend.domain.favorite.repository.FavoriteRepository;
import cloud.picon.backend.domain.favorite.service.FavoriteService;
import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.domain.member.repository.MemberRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FavoriteServiceTest {

    @ParameterizedTest
    @CsvSource({
            "1062, favorite.uk_other_unique",
            "1062, favorite.uk_favorite_member_expert_other",
            "1452, fk_favorite_member",
            "1048, created_at"
    })
    void preservesUnrelatedDatabaseErrors(int vendorCode, String constraint) {
        var favorites = mock(FavoriteRepository.class);
        var experts = mock(ExpertRepository.class);
        var expertDetails = mock(ExpertDetailRepository.class);
        var members = mock(MemberRepository.class);
        var service = new FavoriteService(favorites, experts, expertDetails, members);
        when(expertDetails.existsVisibleExpert(10L)).thenReturn(true);
        when(experts.findById(10L)).thenReturn(Optional.of(mock(Expert.class)));
        when(members.findById(20L)).thenReturn(Optional.of(mock(Member.class)));

        var sql = new SQLException("constraint violation", "23000", vendorCode);
        var violation = new ConstraintViolationException("constraint violation", sql, constraint);
        var failure = new DataIntegrityViolationException("could not save favorite", violation);
        when(favorites.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.add(10, 20)).isSameAs(failure);
    }
}
