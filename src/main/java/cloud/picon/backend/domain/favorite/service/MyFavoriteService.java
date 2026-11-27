package cloud.picon.backend.domain.favorite.service;

import cloud.picon.backend.domain.expert.service.ExpertImageUrls;
import cloud.picon.backend.domain.expert.service.ExpertProfileImageValidator;
import cloud.picon.backend.domain.favorite.dto.MyFavoriteResponse;
import cloud.picon.backend.domain.favorite.repository.MyFavoriteRepository;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyFavoriteService {

    private final MyFavoriteRepository repository;
    private final ExpertProfileImageValidator profileImageValidator;
    private final ExpertImageUrls imageUrls;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<MyFavoriteResponse> find(long memberId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var favorites = repository.findPage(memberId, page, size);
        for (var favorite : favorites.content()) {
            profileImageValidator.validate(favorite.expertId(), favorite.profileImageKey());
        }
        Map<String, String> urls = new HashMap<>();
        var content = favorites.content().stream().map(favorite -> new MyFavoriteResponse(
                favorite.expertId(), favorite.nickname(), imageUrls.resolve(favorite.profileImageKey(), urls),
                favorite.introduction(), favorite.startingPrice(), favorite.rating(), favorite.reviewCount(), true))
                .toList();
        return PageResponse.of(content, page, size, favorites.totalElements());
    }
}
