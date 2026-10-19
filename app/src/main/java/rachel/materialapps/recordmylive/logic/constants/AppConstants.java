package rachel.materialapps.recordmylive.logic.constants;

public class AppConstants {
    private AppConstants(){}
    /** 每天最多记录的演出数量 */
    public static final int MAX_CONCERT_PER_DAY = 5;

    /** 整个数据库最多保存的演出数量 */
    public static final int MAX_TOTAL_CONCERT = 5000;

    /** 每场演出最多记录歌手数量 */
    public static final int MAX_ARTIST_COUNT = 30;

    /** 每场演出最多记录歌单数量 */
    public static final int MAX_SONG_COUNT = 200;

    /** 备注最大长度 */
    public static final int MAX_NOTE_LENGTH = 1000;
}
