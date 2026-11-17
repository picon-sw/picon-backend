package cloud.picon.backend.global.config;

import cloud.picon.backend.global.s3.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    @Lazy
    public S3Presigner s3Presigner(S3Properties properties) {
        S3Presigner.Builder builder = S3Presigner.builder();
        if (StringUtils.hasText(properties.region())) {
            builder.region(Region.of(properties.region()));
        }
        return builder.build();
    }
}
