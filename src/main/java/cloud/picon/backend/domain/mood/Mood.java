package cloud.picon.backend.domain.mood;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Mood {
    BRIGHT("밝음"),
    NATURAL("자연스러움"),
    CALM("차분함"),
    SOPHISTICATED("세련됨"),
    DREAMY("몽환적"),
    DISTINCTIVE("개성적"),
    DRAMATIC("극적 표현");

    private final String label;
}