package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.entity.Expert;
import cloud.picon.backend.domain.expert.entity.ExpertPhotoStyle;
import cloud.picon.backend.domain.expert.entity.ExpertRegion;
import cloud.picon.backend.domain.expert.entity.ExpertSpecialty;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.favorite.entity.Favorite;
import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.domain.photostyle.entity.PhotoStyle;
import cloud.picon.backend.domain.region.entity.Region;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpertReferenceEntityIntegrationTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void hibernateCreatesAllColumnsWithErdTypesNullabilityAndNoDefaults() {
        assertColumns("EXPERT_SPECIALTY", "EXPERT_SPECIALTY_ID BIGINT NO YES",
                "MEMBER_ID BIGINT NO NO", "NAME CHARACTER VARYING(20) NO NO", "BASE_PRICE INTEGER NO NO",
                "CREATED_AT TIMESTAMP NO NO", "UPDATED_AT TIMESTAMP NO NO");
        assertColumns("PHOTO_STYLE", "PHOTO_STYLE_ID BIGINT NO YES", "NAME CHARACTER VARYING(20) NO NO");
        assertColumns("EXPERT_PHOTO_STYLE", "EXPERT_PHOTO_STYLE_ID BIGINT NO YES", "MEMBER_ID BIGINT NO NO",
                "PHOTO_STYLE_ID BIGINT NO NO", "CREATED_AT TIMESTAMP NO NO");
        assertColumns("REGION", "REGION_CODE BIGINT NO NO", "FIRST_DEPTH CHARACTER VARYING(20) NO NO",
                "SECOND_DEPTH CHARACTER VARYING(20) YES NO", "THIRD_DEPTH CHARACTER VARYING(20) YES NO");
        assertColumns("EXPERT_REGION", "EXPERT_REGION_ID BIGINT NO YES", "MEMBER_ID BIGINT NO NO",
                "REGION_CODE BIGINT NO NO", "CREATED_AT TIMESTAMP NO NO");
        assertColumns("FAVORITE", "FAVORITE_ID BIGINT NO YES", "MEMBER_ID BIGINT NO NO",
                "EXPERT_MEMBER_ID BIGINT NO NO", "CREATED_AT TIMESTAMP NO NO");
    }

    @Test
    void hibernateCreatesPrimaryAndForeignKeys() {
        assertThat(jdbc.queryForList("""
                SELECT k.TABLE_NAME || '.' || k.COLUMN_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS c
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k ON k.CONSTRAINT_NAME = c.CONSTRAINT_NAME
                    AND k.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA
                WHERE c.CONSTRAINT_TYPE = 'PRIMARY KEY' AND c.TABLE_SCHEMA = 'PUBLIC'
                    AND c.TABLE_NAME IN ('EXPERT_SPECIALTY', 'PHOTO_STYLE', 'EXPERT_PHOTO_STYLE',
                        'REGION', 'EXPERT_REGION', 'FAVORITE')
                """, String.class)).containsExactlyInAnyOrder(
                "EXPERT_SPECIALTY.EXPERT_SPECIALTY_ID", "PHOTO_STYLE.PHOTO_STYLE_ID",
                "EXPERT_PHOTO_STYLE.EXPERT_PHOTO_STYLE_ID", "REGION.REGION_CODE",
                "EXPERT_REGION.EXPERT_REGION_ID", "FAVORITE.FAVORITE_ID");

        assertThat(jdbc.queryForList("""
                SELECT f.TABLE_NAME || '.' || f.COLUMN_NAME || ' -> ' || p.TABLE_NAME || '.' || p.COLUMN_NAME
                FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS r
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE f ON f.CONSTRAINT_NAME = r.CONSTRAINT_NAME
                    AND f.CONSTRAINT_SCHEMA = r.CONSTRAINT_SCHEMA
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE p ON p.CONSTRAINT_NAME = r.UNIQUE_CONSTRAINT_NAME
                    AND p.CONSTRAINT_SCHEMA = r.UNIQUE_CONSTRAINT_SCHEMA
                    AND p.ORDINAL_POSITION = f.POSITION_IN_UNIQUE_CONSTRAINT
                WHERE f.TABLE_SCHEMA = 'PUBLIC' AND f.TABLE_NAME IN
                    ('EXPERT_SPECIALTY', 'EXPERT_PHOTO_STYLE', 'EXPERT_REGION', 'FAVORITE')
                """, String.class)).containsExactlyInAnyOrder(
                "EXPERT_SPECIALTY.MEMBER_ID -> EXPERT.MEMBER_ID",
                "EXPERT_PHOTO_STYLE.MEMBER_ID -> EXPERT.MEMBER_ID",
                "EXPERT_PHOTO_STYLE.PHOTO_STYLE_ID -> PHOTO_STYLE.PHOTO_STYLE_ID",
                "EXPERT_REGION.MEMBER_ID -> EXPERT.MEMBER_ID",
                "EXPERT_REGION.REGION_CODE -> REGION.REGION_CODE",
                "FAVORITE.MEMBER_ID -> MEMBER.MEMBER_ID",
                "FAVORITE.EXPERT_MEMBER_ID -> EXPERT.MEMBER_ID");
    }

    @Test
    void persistsAllEntitiesWithGeneratedIdsAssignedRegionCodeAndAuditing() throws Exception {
        Expert expert = persistExpert();
        Member viewer = persistMember(987657L);
        PhotoStyle style = new PhotoStyle("Test style");
        Region region = new Region(1165000000L, "\uc11c\uc6b8\ud2b9\ubcc4\uc2dc", null, null);
        entityManager.persist(style);
        entityManager.persist(region);

        ExpertSpecialty specialty = new ExpertSpecialty(expert, Specialty.SHOOTING, 80000);
        ExpertPhotoStyle expertStyle = new ExpertPhotoStyle(expert, style);
        ExpertRegion expertRegion = new ExpertRegion(expert, region);
        Favorite favorite = new Favorite(viewer, expert);
        entityManager.persist(specialty);
        entityManager.persist(expertStyle);
        entityManager.persist(expertRegion);
        entityManager.persist(favorite);
        entityManager.flush();
        entityManager.clear();

        assertThat(specialty.getId()).isPositive();
        assertThat(style.getId()).isPositive();
        assertThat(expertStyle.getId()).isPositive();
        assertThat(expertRegion.getId()).isPositive();
        assertThat(favorite.getId()).isPositive();
        var loadedRegion = entityManager.find(Region.class, 1165000000L);
        assertThat(loadedRegion.getFirstDepth()).isEqualTo("\uc11c\uc6b8\ud2b9\ubcc4\uc2dc");
        assertThat(loadedRegion.getSecondDepth()).isNull();
        assertThat(loadedRegion.getThirdDepth()).isNull();

        var loadedStyle = entityManager.find(ExpertPhotoStyle.class, expertStyle.getId());
        var loadedExpertRegion = entityManager.find(ExpertRegion.class, expertRegion.getId());
        var loadedFavorite = entityManager.find(Favorite.class, favorite.getId());
        var loadedSpecialty = entityManager.find(ExpertSpecialty.class, specialty.getId());

        var persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistence.isLoaded(loadedStyle, "photoStyle")).isFalse();
        assertThat(persistence.isLoaded(loadedExpertRegion, "expert")).isFalse();
        assertThat(persistence.isLoaded(loadedFavorite, "member")).isFalse();
        assertThat(loadedStyle.getPhotoStyle().getName()).isEqualTo("Test style");
        assertThat(loadedExpertRegion.getRegion().getRegionCode()).isEqualTo(region.getRegionCode());
        assertThat(loadedFavorite.getMember().getId()).isEqualTo(viewer.getId());
        assertThat(loadedFavorite.getExpert().getId()).isEqualTo(expert.getId());
        assertThat(loadedSpecialty.getName()).isEqualTo(Specialty.SHOOTING);
        assertThat(loadedStyle.getCreatedAt()).isNotNull();
        assertThat(loadedExpertRegion.getCreatedAt()).isNotNull();
        assertThat(loadedFavorite.getCreatedAt()).isNotNull();
        assertThat(loadedSpecialty.getCreatedAt()).isNotNull();
        assertThat(loadedSpecialty.getUpdatedAt()).isNotNull();
    }

    @Test
    void specialtyUpdatePreservesCreationTimeAndRefreshesModificationTime() throws Exception {
        ExpertSpecialty specialty = new ExpertSpecialty(persistExpert(), Specialty.EDITING, 30000);
        entityManager.persist(specialty);
        entityManager.flush();
        LocalDateTime oldTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        jdbc.update("UPDATE expert_specialty SET created_at = ?, updated_at = ? WHERE expert_specialty_id = ?",
                oldTime, oldTime, specialty.getId());
        entityManager.clear();

        var loaded = entityManager.find(ExpertSpecialty.class, specialty.getId());
        LocalDateTime originalCreatedAt = loaded.getCreatedAt();
        LocalDateTime originalUpdatedAt = loaded.getUpdatedAt();
        ReflectionTestUtils.setField(loaded, "basePrice", 35000);
        entityManager.flush();
        entityManager.clear();

        var updated = entityManager.find(ExpertSpecialty.class, specialty.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getBasePrice()).isEqualTo(35000);
    }

    @Test
    void specialtyIsUniquePerExpertAndName() throws Exception {
        Expert firstExpert = persistExpert();
        Expert secondExpert = persistExpert(987658L);
        entityManager.persist(new ExpertSpecialty(firstExpert, Specialty.SHOOTING, 80000));
        entityManager.persist(new ExpertSpecialty(firstExpert, Specialty.EDITING, 30000));
        entityManager.persist(new ExpertSpecialty(secondExpert, Specialty.SHOOTING, 90000));
        entityManager.flush();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO expert_specialty (member_id, name, base_price, created_at, updated_at)
                VALUES (?, 'SHOOTING', 100000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, firstExpert.getId()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void regionIsUniquePerExpertAndRegionCode() throws Exception {
        Expert firstExpert = persistExpert();
        Expert secondExpert = persistExpert(987658L);
        Region firstRegion = new Region(1165000000L, "Seoul", "Seocho", null);
        Region secondRegion = new Region(1168000000L, "Seoul", "Gangnam", null);
        entityManager.persist(firstRegion);
        entityManager.persist(secondRegion);
        entityManager.persist(new ExpertRegion(firstExpert, firstRegion));
        entityManager.persist(new ExpertRegion(firstExpert, secondRegion));
        entityManager.persist(new ExpertRegion(secondExpert, firstRegion));
        entityManager.flush();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO expert_region (member_id, region_code, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, firstExpert.getId(), firstRegion.getRegionCode()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void photoStyleIsUniquePerExpertAndPhotoStyleId() throws Exception {
        Expert firstExpert = persistExpert();
        Expert secondExpert = persistExpert(987658L);
        PhotoStyle firstStyle = new PhotoStyle("First style");
        PhotoStyle secondStyle = new PhotoStyle("Second style");
        entityManager.persist(firstStyle);
        entityManager.persist(secondStyle);
        entityManager.persist(new ExpertPhotoStyle(firstExpert, firstStyle));
        entityManager.persist(new ExpertPhotoStyle(firstExpert, secondStyle));
        entityManager.persist(new ExpertPhotoStyle(secondExpert, firstStyle));
        entityManager.flush();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO expert_photo_style (member_id, photo_style_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, firstExpert.getId(), firstStyle.getId()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void favoriteIsUniquePerMemberAndExpert() throws Exception {
        Expert firstExpert = persistExpert();
        Expert secondExpert = persistExpert(987658L);
        Member firstViewer = persistMember(987657L);
        Member secondViewer = persistMember(987659L);
        entityManager.persist(new Favorite(firstViewer, firstExpert));
        entityManager.persist(new Favorite(firstViewer, secondExpert));
        entityManager.persist(new Favorite(secondViewer, firstExpert));
        entityManager.flush();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO favorite (member_id, expert_member_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, firstViewer.getId(), firstExpert.getId()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void rejectsLinksToMissingParentsAndToMembersWithoutAnExpertRow() throws Exception {
        Expert expert = persistExpert();
        Member viewer = persistMember(987657L);
        entityManager.flush();

        assertRejected("""
                INSERT INTO expert_specialty (member_id, name, base_price, created_at, updated_at)
                VALUES (?, 'SHOOTING', 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, viewer.getId());
        assertRejected("""
                INSERT INTO expert_photo_style (member_id, photo_style_id, created_at)
                VALUES (?, -1, CURRENT_TIMESTAMP)
                """, expert.getId());
        assertRejected("""
                INSERT INTO expert_region (member_id, region_code, created_at)
                VALUES (?, -1, CURRENT_TIMESTAMP)
                """, expert.getId());
        assertRejected("""
                INSERT INTO favorite (member_id, expert_member_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, viewer.getId(), viewer.getId());
        assertRejected("""
                INSERT INTO favorite (member_id, expert_member_id, created_at)
                VALUES (-1, ?, CURRENT_TIMESTAMP)
                """, expert.getId());
    }

    private void assertColumns(String table, String... expected) {
        var columns = jdbc.query("""
                SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, IS_IDENTITY, COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?
                """, (rs, row) -> {
            assertThat(rs.getString("COLUMN_DEFAULT"))
                    .as("%s.%s default", table, rs.getString("COLUMN_NAME"))
                    .isNull();
            String length = rs.getString("CHARACTER_MAXIMUM_LENGTH");
            String type = rs.getString("DATA_TYPE");
            if (length != null) {
                type += "(" + length + ")";
            }
            return rs.getString("COLUMN_NAME") + " " + type + " "
                    + rs.getString("IS_NULLABLE") + " " + rs.getString("IS_IDENTITY");
        }, table);
        assertThat(columns).as("%s columns", table).containsExactlyInAnyOrder(expected);
    }

    private void assertRejected(String sql, Object... args) {
        assertThatThrownBy(() -> jdbc.update(sql, args)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Member persistMember(long kakaoId) {
        Member member = Member.createKakaoMember(kakaoId);
        entityManager.persist(member);
        return member;
    }

    private Expert persistExpert() throws Exception {
        return persistExpert(987656L);
    }

    private Expert persistExpert(long kakaoId) throws Exception {
        var constructor = Expert.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Expert expert = constructor.newInstance();
        ReflectionTestUtils.setField(expert, "member", persistMember(kakaoId));
        entityManager.persist(expert);
        return expert;
    }
}
