package cloud.picon.backend.domain.expert.repository;

public interface PortfolioImageView {
    Long getImageId();
    Long getExpertId();
    String getExpertNickname();
    String getImageKey();
    String getSpecialty();
}
