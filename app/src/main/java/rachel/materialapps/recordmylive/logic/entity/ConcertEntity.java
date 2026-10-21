package rachel.materialapps.recordmylive.logic.entity;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Data;
import lombok.ToString;
import rachel.materialapps.recordmylive.logic.constants.ConcertStatus;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;
import rachel.materialapps.recordmylive.logic.constants.PurchaseChannel;
import rachel.materialapps.recordmylive.logic.entity.handler.ArtistListConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.ConcertStatusConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.ConcertTypeConverter;
import rachel.materialapps.recordmylive.logic.entity.handler.PurchaseChannelConverter;

@Data
@ToString
@Entity(tableName = "concert",indices = {
        @Index("date"),
        @Index("city"),
        @Index("status"),
        @Index("favorite")
})
@TypeConverters({
        ConcertTypeConverter.class,
        PurchaseChannelConverter.class,
        ConcertStatusConverter.class,
        ArtistListConverter.class
})

public class ConcertEntity implements Serializable {

    /**
     * 主键
     */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * 演出名称
     */
    //@NonNull
    private String name;

    /**
     * 演出类型
     */
    //@NonNull
    private ConcertType type;

    /**
     * 演出日期（必填；新建草稿须在保存前设置）
     */
    //@NonNull
    private LocalDate date;

    public void setDate(@NonNull LocalDate date) {
        this.date = Objects.requireNonNull(date, "演出日期不能为空");
    }

    public void requireDate() {
        Objects.requireNonNull(date, "演出日期不能为空");
    }

    /**
     * 演出时间（允许为空）
     */
    private LocalTime time;

    /** 开票日期和时间均可为空，时间填写时须同时填写日期。 */
    private LocalDate ticketSaleDate;
    private LocalTime ticketSaleTime;

    /**
     * 演出时长（单位：分钟，允许为空）
     */
    private Integer durationMinutes;

    /**
     * 城市
     */
    //@NonNull
    private String city;

    /**
     * 场馆
     */
    //@NonNull
    private String venue;

    /**
     * 座位信息
     */
    @Embedded(prefix = "seat_")
    private SeatInfo seat;

    /** 票面票价（单位：分） */
    private long faceValuePriceCents;

    /** 实付票价（单位：分） */
    private long paidPriceCents;

    /**
     * 歌手（支持多人）
     */
    //@NonNull
    private List<String> artists;

    /**
     * 购票渠道
     */
    //@NonNull
    private PurchaseChannel purchaseChannel;

    /**
     * 演出状态
     */
    //@NonNull
    private ConcertStatus status;

    /**
     * 收藏
     */
    private boolean favorite;

    /**
     * 备注
     */
    private String note;


    private boolean ticketCollected;


    private String coverPath;

    public static ConcertEntity createEmpty() {
        ConcertEntity entity = new ConcertEntity();
        entity.setArtists(new ArrayList<>());
        entity.setSeat(new SeatInfo());
        entity.setStatus(ConcertStatus.PLANNED);
        entity.setPurchaseChannel(PurchaseChannel.OTHER);
        entity.setPaidPriceCents(0);
        entity.setFaceValuePriceCents(0);
        entity.setDurationMinutes(null);
        entity.setFavorite(false);
        entity.setTicketCollected(false);
        return entity;
    }
}
