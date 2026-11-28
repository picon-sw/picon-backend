package cloud.picon.backend.domain.request;

import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3UploadUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestImageUploadIntegrationTest {

    private static final String PATH = "/api/requests/images/upload-urls";
    private static final long MEMBER = 7;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @MockitoBean S3UploadUrlProvider uploadUrlProvider;

    private String authorization;

    @BeforeEach
    void setUp() {
        when(uploadUrlProvider.getUploadUrl(anyString(), anyString(), anyLong()))
                .thenAnswer(call -> "https://upload.example/" + call.getArgument(0));
        authorization = "Bearer " + jwtProvider.createAccessToken(MEMBER);
    }

    private ResultActions request(String body) throws Exception {
        return mvc.perform(post(PATH).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String image(String type, String contentType, long size) {
        return """
                {"imageType":"%s","contentType":"%s","fileSize":%d}""".formatted(type, contentType, size);
    }

    private static String body(List<String> images) {
        return "{\"images\":[" + String.join(",", images) + "]}";
    }

    private static List<String> toEdit(int count) {
        List<String> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            images.add(image("TO_EDIT", "image/jpeg", 1000));
        }
        return images;
    }

    @Test
    void issuesUrlsInRequestOrderWithKeysOwnedByMember() throws Exception {
        var result = request(body(List.of(
                image("REFERENCE", "image/jpeg", 1000),
                image("TO_EDIT", "image/png", 2000),
                image("TO_EDIT", "image/webp", MAX_FILE_SIZE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.expiresIn").value(600))
                .andExpect(jsonPath("$.result.images[*].imageType").value(contains("REFERENCE", "TO_EDIT", "TO_EDIT")));

        var keys = ArgumentCaptor.forClass(String.class);
        var contentTypes = ArgumentCaptor.forClass(String.class);
        var sizes = ArgumentCaptor.forClass(Long.class);
        verify(uploadUrlProvider, times(3)).getUploadUrl(keys.capture(), contentTypes.capture(), sizes.capture());
        assertThat(contentTypes.getAllValues()).containsExactly("image/jpeg", "image/png", "image/webp");
        assertThat(sizes.getAllValues()).containsExactly(1000L, 2000L, MAX_FILE_SIZE);
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
        assertThat(keys.getAllValues().get(0)).matches("requests/" + MEMBER + "/" + UUID + "\\.jpg");
        assertThat(keys.getAllValues().get(1)).matches("requests/" + MEMBER + "/" + UUID + "\\.png");
        assertThat(keys.getAllValues().get(2)).matches("requests/" + MEMBER + "/" + UUID + "\\.webp");
        for (int i = 0; i < 3; i++) {
            String key = keys.getAllValues().get(i);
            result.andExpect(jsonPath("$.result.images[" + i + "].imageKey").value(key))
                    .andExpect(jsonPath("$.result.images[" + i + "].uploadUrl").value("https://upload.example/" + key));
        }
    }

    @Test
    void allowsOneReferenceAndThirtyToEditImages() throws Exception {
        List<String> images = new ArrayList<>(toEdit(30));
        images.add(image("REFERENCE", "image/png", 1));
        request(body(images))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.images", hasSize(31)));
        verify(uploadUrlProvider, times(31)).getUploadUrl(anyString(), anyString(), anyLong());
    }

    @Test
    void rejectsMoreThanThirtyToEditImages() throws Exception {
        request(body(toEdit(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(uploadUrlProvider);
    }

    @Test
    void rejectsMoreThanThirtyOneImagesInTotal() throws Exception {
        List<String> images = new ArrayList<>(toEdit(30));
        images.add(image("REFERENCE", "image/png", 1));
        images.add(image("REFERENCE", "image/png", 1));
        request(body(images))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(uploadUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"images\":null}",
            "{\"images\":[]}",
            "{\"images\":[null]}",
            "{\"images\":[{\"imageType\":\"REFERENCE\",\"contentType\":\"image/jpeg\",\"fileSize\":1},"
                    + "{\"imageType\":\"REFERENCE\",\"contentType\":\"image/jpeg\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/gif\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/heic\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"IMAGE/JPEG\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg; charset=utf-8\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg\",\"fileSize\":0}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg\",\"fileSize\":-1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg\",\"fileSize\":10485761}]}",
            "{\"images\":[{\"imageType\":\"OTHER\",\"contentType\":\"image/jpeg\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"reference\",\"contentType\":\"image/jpeg\",\"fileSize\":1}]}",
            "{\"images\":[{\"contentType\":\"image/jpeg\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"fileSize\":1}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg\"}]}",
            "{\"images\":[{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/jpeg\",\"fileSize\":1},"
                    + "{\"imageType\":\"TO_EDIT\",\"contentType\":\"image/gif\",\"fileSize\":1}]}"
    })
    void rejectsInvalidRequestsWithoutIssuingAnyUrl(String body) throws Exception {
        request(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(uploadUrlProvider);
    }

    @Test
    void rejectsMissingBody() throws Exception {
        mvc.perform(post(PATH).header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(uploadUrlProvider);
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(image("TO_EDIT", "image/jpeg", 1)))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(uploadUrlProvider);
    }

    @Test
    void hidesUrlSigningFailures() throws Exception {
        when(uploadUrlProvider.getUploadUrl(anyString(), anyString(), anyLong()))
                .thenThrow(new IllegalStateException("Unable to generate upload URL"));
        request(body(List.of(image("TO_EDIT", "image/jpeg", 1))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }
}