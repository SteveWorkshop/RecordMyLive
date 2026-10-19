package rachel.materialapps.recordmylive.logic.constants;

import lombok.Getter;

@Getter
public enum ConcertType {

    CONCERT("巡演"),
    BUSINESS("商演"),

    MIXTURE("拼盘"),
    MUSIC_FESTIVAL("音乐节"),

    LIVE_HOUSE("LiveHouse"),

    FAN_MEETING("见面会"),

    THEATER("音乐幕剧"),

    OTHER("其他");

    private final String displayName;

    ConcertType(String displayName) {
        this.displayName = displayName;
    }

}
