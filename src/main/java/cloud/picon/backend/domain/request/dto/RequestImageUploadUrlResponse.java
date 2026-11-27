package cloud.picon.backend.domain.request.dto;

import cloud.picon.backend.domain.request.entity.RequestImageType;

import java.util.List;

public record RequestImageUploadUrlResponse(long expiresIn, List<Image> images) {

    public record Image(RequestImageType imageType, String imageKey, String uploadUrl) {
    }
}