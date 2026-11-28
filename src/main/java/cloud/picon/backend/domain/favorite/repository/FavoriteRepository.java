package cloud.picon.backend.domain.favorite.repository;

import cloud.picon.backend.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
}
