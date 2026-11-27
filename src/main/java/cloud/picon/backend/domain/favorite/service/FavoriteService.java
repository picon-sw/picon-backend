package cloud.picon.backend.domain.favorite.service;

import cloud.picon.backend.domain.expert.code.ExpertErrorCode;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertRepository;
import cloud.picon.backend.domain.favorite.code.FavoriteErrorCode;
import cloud.picon.backend.domain.favorite.entity.Favorite;
import cloud.picon.backend.domain.favorite.repository.FavoriteRepository;
import cloud.picon.backend.domain.member.repository.MemberRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final Pattern H2_FAVORITE_UNIQUE_INDEX = Pattern.compile(
            "(?i)PUBLIC\\.UK_FAVORITE_MEMBER_EXPERT INDEX PUBLIC\\.UK_FAVORITE_MEMBER_EXPERT_INDEX_[0-9A-F]+");

    private final FavoriteRepository favorites;
    private final ExpertRepository experts;
    private final ExpertDetailRepository expertDetails;
    private final MemberRepository members;

    @Transactional
    public void add(long expertId, long memberId) {
        if (expertId <= 0 || expertId == memberId) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (!expertDetails.existsVisibleExpert(expertId)) {
            throw new BusinessException(ExpertErrorCode.EXPERT_NOT_FOUND);
        }
        var expert = experts.findById(expertId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_NOT_FOUND));
        var member = members.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        try {
            favorites.saveAndFlush(new Favorite(member, expert));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateFavorite(exception)) {
                throw new BusinessException(FavoriteErrorCode.ALREADY_FAVORITED);
            }
            throw exception;
        }
    }

    @Transactional
    public void remove(long expertId, long memberId) {
        if (expertId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        favorites.deleteByMemberAndExpertIds(memberId, expertId);
    }

    private static boolean isDuplicateFavorite(DataIntegrityViolationException exception) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName();
                // MySQL can qualify the key name with its table.
                return switch (violation.getErrorCode()) {
                    case 1062 -> name.equalsIgnoreCase("uk_favorite_member_expert")
                            || name.equalsIgnoreCase("favorite.uk_favorite_member_expert");
                    // H2 reports the backing index name for a generated UNIQUE constraint.
                    case 23505 -> H2_FAVORITE_UNIQUE_INDEX.matcher(name).matches();
                    default -> false;
                };
            }
        }
        return false;
    }
}
