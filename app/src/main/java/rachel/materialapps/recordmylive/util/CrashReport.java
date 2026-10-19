package rachel.materialapps.recordmylive.util;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CrashReport implements Serializable {
    /**
     * Crash ID
     */
    private String crashId;

    /**
     * 崩溃时间
     */
    private LocalDateTime crashTime;

    /**
     * 线程名称
     */
    private String threadName;

    /**
     * 异常类型
     */
    private String exceptionName;

    /**
     * 异常信息
     */
    private String exceptionMessage;

    /**
     * 完整堆栈
     */
    private String stackTrace;

    /**
     * App版本
     */
    private String appVersion;

    /**
     * VersionCode
     */
    private long versionCode;

    /**
     * 包名
     */
    private String packageName;

    /**
     * Android版本
     */
    private String androidVersion;

    /**
     * API Level
     */
    private int sdkInt;

    /**
     * OEM名称
     */
    private String manufacturer;

    /**
     * OEM 型号
     */
    private String model;

    /**
     * CPU ABI
     */
    private String abi;

    /**
     * 系统语言
     */
    private String locale;

    private String brand;

    private String device;

    private String buildId;
}
