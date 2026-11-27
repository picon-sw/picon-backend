package cloud.picon.backend.global.s3;

public interface S3UploadUrlProvider {

    // 지정한 Content-Type과 크기의 파일만 올릴 수 있는 PUT 주소
    String getUploadUrl(String imageKey, String contentType, long contentLength);
}