package cloud.picon.backend.domain.expert.repository;

import cloud.picon.backend.domain.expert.entity.ExpertImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpertImageRepository extends JpaRepository<ExpertImage, Long> {

    String FROM_AND_FILTER = """
            FROM expert_image i
            JOIN member m ON m.member_id = i.member_id
            WHERE m.nickname IS NOT NULL AND TRIM(m.nickname) <> ''
              AND EXISTS (SELECT 1 FROM expert e WHERE e.member_id = i.member_id)
              AND i.specialty IN ('SHOOTING', 'EDITING')
              AND (:specialty IS NULL OR i.specialty = :specialty)
            """;
    String COUNT_QUERY = "SELECT COUNT(*) " + FROM_AND_FILTER;

    // Expert Entity 없이 등록 여부를 검사하고, DTO projection으로 회원별 추가 조회를 방지한다.
    @Query(value = """
            SELECT i.expert_image_id AS imageId, i.member_id AS expertId,
                   m.nickname AS expertNickname, i.image_key AS imageKey, i.specialty AS specialty
            """ + FROM_AND_FILTER + """
            ORDER BY i.created_at DESC, i.expert_image_id DESC
            """, countQuery = COUNT_QUERY, nativeQuery = true)
    Page<PortfolioImageView> findPortfolioImages(@Param("specialty") String specialty, Pageable pageable);

    @Query(value = COUNT_QUERY, nativeQuery = true)
    long countPortfolioImages(@Param("specialty") String specialty);

    String EXPERT_IMAGES_FROM_AND_FILTER = """
            FROM expert_image i
            WHERE i.member_id = :expertId AND (:specialty IS NULL OR i.specialty = :specialty)
            """;
    String EXPERT_IMAGES_COUNT_QUERY = "SELECT COUNT(*) " + EXPERT_IMAGES_FROM_AND_FILTER;

    // DATETIME을 문자로 읽어 native JDBC TIMESTAMP 변환의 시간대 이동을 피한다.
    @Query(value = """
            SELECT i.expert_image_id AS imageId, i.image_key AS imageKey,
                   i.specialty AS specialty, CAST(i.created_at AS CHAR(32)) AS createdAtText
            """ + EXPERT_IMAGES_FROM_AND_FILTER + """
            ORDER BY i.created_at DESC, i.expert_image_id DESC
            """, countQuery = EXPERT_IMAGES_COUNT_QUERY, nativeQuery = true)
    Page<ExpertImageView> findExpertImages(@Param("expertId") long expertId,
                                         @Param("specialty") String specialty, Pageable pageable);

    @Query(value = EXPERT_IMAGES_COUNT_QUERY, nativeQuery = true)
    long countExpertImages(@Param("expertId") long expertId, @Param("specialty") String specialty);
}
