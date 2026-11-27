package cloud.picon.backend.domain.expert.repository;

public final class ExpertVisibility {

    // Apply the same Unicode White_Space set in MySQL ICU and H2 Java regular expressions.
    public static final String NON_WHITESPACE_PATTERN =
            "[^\t-\r \u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]";

    // Uses m as the member table alias.
    public static final String NICKNAME_CONDITION =
            "m.nickname IS NOT NULL AND REGEXP_LIKE(m.nickname, :nonWhitespacePattern)";

    private ExpertVisibility() {
    }
}
