package cloud.picon.backend.domain.favorite.repository;

import cloud.picon.backend.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.member.id = :memberId AND f.expert.id = :expertId")
    void deleteByMemberAndExpertIds(@Param("memberId") long memberId, @Param("expertId") long expertId);
}
