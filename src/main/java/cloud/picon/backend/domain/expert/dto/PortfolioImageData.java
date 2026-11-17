package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;

// DB 트랜잭션 안에서 projection을 값으로 확정한다. S3 URL은 트랜잭션 밖에서 생성한다.
public record PortfolioImageData(
        Long imageId,
        Long expertId,
        String expertNickname,
        String imageKey,
        Specialty specialty
) {
}
