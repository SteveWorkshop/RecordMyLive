package rachel.materialapps.recordmylive.logic.constants;

import lombok.Getter;

@Getter
public enum PurchaseChannel {
    DAMAI("大麦"),

    MAOYAN("猫眼"),

    SHOW_START("秀动"),

    TIKTOK("抖音"),

    BILI_BILI("B站"),

    OFFICIAL("主办方官网"),

    IDLE_FISH("二手渠道"),

    OTHER("其他");

    private final String displayName;

    PurchaseChannel(String displayName) {
        this.displayName = displayName;
    }

}
