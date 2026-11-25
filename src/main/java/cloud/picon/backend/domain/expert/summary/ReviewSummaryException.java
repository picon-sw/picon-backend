package cloud.picon.backend.domain.expert.summary;

public final class ReviewSummaryException extends RuntimeException {

    public enum Kind {
        CONFIGURATION, AUTHENTICATION, RATE_LIMIT, PROVIDER, HTTP, TIMEOUT, TRANSPORT, INVALID_RESPONSE
    }

    private final Kind kind;

    ReviewSummaryException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
