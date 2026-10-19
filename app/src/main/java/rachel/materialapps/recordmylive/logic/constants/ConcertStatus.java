package rachel.materialapps.recordmylive.logic.constants;

import lombok.Getter;

@Getter
public enum ConcertStatus {
    PLANNED("未开始"),

    ATTENDED("已观看"),

    CANCELLED("已取消");

    private final String displayName;

    ConcertStatus(String displayName) {
        this.displayName = displayName;
    }

}
