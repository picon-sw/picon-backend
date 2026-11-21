package cloud.picon.backend.global.apipayload;

import java.util.List;

public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, long totalPages, boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        long totalPages = totalElements / size + (totalElements % size == 0 ? 0 : 1);
        return new PageResponse<>(content, page, size, totalElements, totalPages, (long) page + 1 >= totalPages);
    }
}
