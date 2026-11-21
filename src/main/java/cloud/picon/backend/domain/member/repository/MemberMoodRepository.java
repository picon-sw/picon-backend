package cloud.picon.backend.domain.member.repository;

import cloud.picon.backend.domain.member.entity.MemberMood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberMoodRepository extends JpaRepository<MemberMood, Long> {
}