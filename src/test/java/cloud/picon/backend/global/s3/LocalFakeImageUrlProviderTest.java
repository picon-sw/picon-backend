package cloud.picon.backend.global.s3;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFakeImageUrlProviderTest {

    @Test
    void encodesSpecialCharactersWithoutChangingHostOrPathSeparators() {
        String key = "portfolio/\uC0AC\uC9C4 a+b?#%&=;.jpg";
        String url = new LocalFakeImageUrlProvider().getImageUrl(key);
        assertThat(url).startsWith("https://local.invalid/portfolio/%EC%82%AC%EC%A7%84%20a+b")
                .contains("%3F%23%25");
        URI uri = URI.create(url);
        assertThat(uri.getHost()).isEqualTo("local.invalid");
        assertThat(uri.getPath()).isEqualTo("/" + key);
        assertThat(uri.getQuery()).isNull();
        assertThat(uri.getFragment()).isNull();
    }

    @Test
    void leadingSlashesCannotChangeTheFixedHost() {
        URI uri = URI.create(new LocalFakeImageUrlProvider().getImageUrl("//external.example/image"));
        assertThat(uri.getHost()).isEqualTo("local.invalid");
    }
}
