package cloud.picon.backend.domain.expert.service;

import java.util.regex.Pattern;

final class ExpertImageKey {

    private ExpertImageKey() {
    }

    static boolean hasUrlPrefix(String key, Pattern urlPrefix) {
        return key != null && urlPrefix.matcher(key.strip()).find();
    }
}
