package cloud.picon.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // 서버 시간대를 UTC로 고정. 로컬(한국 시간)과 배포 서버의 시간 계산을 같게 맞춤
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BackendApplication.class, args);
    }

}