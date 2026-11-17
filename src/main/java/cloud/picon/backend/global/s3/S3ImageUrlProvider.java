package cloud.picon.backend.global.s3;

public interface S3ImageUrlProvider {

    String getImageUrl(String imageKey);
}
