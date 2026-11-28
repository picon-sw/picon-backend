package cloud.picon.backend.domain.request.service;

import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlRequest;
import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlResponse;
import cloud.picon.backend.domain.request.entity.RequestImageType;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.s3.S3Properties;
import cloud.picon.backend.global.s3.S3UploadUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestImageUploadService {

    static final int MAX_REFERENCE_IMAGES = 1;
    static final int MAX_TO_EDIT_IMAGES = 30;

    // 허용 형식과 저장할 확장자
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final S3UploadUrlProvider uploadUrlProvider;
    private final S3Properties properties;

    public RequestImageUploadUrlResponse issueUploadUrls(long memberId, RequestImageUploadUrlRequest request) {
        List<RequestImageUploadUrlRequest.Image> images = request.images();
        long references = images.stream().filter(image -> image.imageType() == RequestImageType.REFERENCE).count();
        long toEdit = images.size() - references;
        if (references > MAX_REFERENCE_IMAGES || toEdit > MAX_TO_EDIT_IMAGES) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // 하나라도 형식이 틀리면 URL을 하나도 만들지 않음
        if (!images.stream().allMatch(image -> EXTENSIONS.containsKey(image.contentType()))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<RequestImageUploadUrlResponse.Image> issued = images.stream().map(image -> {
            String key = RequestImageKey.newKey(memberId, EXTENSIONS.get(image.contentType()));
            String url = uploadUrlProvider.getUploadUrl(key, image.contentType(), image.fileSize());
            return new RequestImageUploadUrlResponse.Image(image.imageType(), key, url);
        }).toList();
        return new RequestImageUploadUrlResponse(properties.presignedUrlExpiration().toSeconds(), issued);
    }

    // 키에 회원 ID를 넣어, 의뢰 등록 때 본인이 발급받은 키인지 확인할 수 있게 함
    static final class RequestImageKey {

        private RequestImageKey() {
        }

        static String prefix(long memberId) {
            return "requests/" + memberId + "/";
        }

        static String newKey(long memberId, String extension) {
            return prefix(memberId) + UUID.randomUUID() + "." + extension;
        }
    }
}