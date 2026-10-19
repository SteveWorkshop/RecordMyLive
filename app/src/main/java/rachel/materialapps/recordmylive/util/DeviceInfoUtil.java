package rachel.materialapps.recordmylive.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DeviceInfoUtil {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DeviceInfoUtil() {

    }

    /**
     * 根据 Throwable 创建 CrashReport
     */
    public static CrashReport fromThrowable(Context context,
                                            Thread thread,
                                            Throwable throwable) {

        CrashReport report = new CrashReport();

        LocalDateTime now = LocalDateTime.now();

        report.setCrashId(now.format(ID_FORMATTER));

        report.setCrashTime(now);

        if (thread != null) {
            report.setThreadName(thread.getName());
        }

        if (throwable != null) {

            report.setExceptionName(
                    throwable.getClass().getName());

            report.setExceptionMessage(
                    throwable.getMessage());

            report.setStackTrace(
                    CrashFormatter.getStackTrace(throwable));

        }

        fill(context, report);

        return report;
    }

    /**
     * 填充设备信息
     */
    public static void fill(Context context,
                            CrashReport report) {

        if (context == null || report == null) {
            return;
        }

        fillApplication(context, report);

        fillAndroid(report);

        fillDevice(report);

        fillLocale(report);
    }

    /**
     * App 信息
     */
    private static void fillApplication(Context context,
                                        CrashReport report) {

        try {

            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            report.setPackageName(info.packageName);

            report.setAppVersion(info.versionName);

            report.setVersionCode(info.getLongVersionCode());

        } catch (Exception ignored) {

        }

    }

    /**
     * Android 信息
     */
    private static void fillAndroid(CrashReport report) {

        report.setAndroidVersion(Build.VERSION.RELEASE);

        report.setSdkInt(Build.VERSION.SDK_INT);

    }

    /**
     * 设备信息
     */
    private static void fillDevice(CrashReport report) {

        report.setManufacturer(Build.MANUFACTURER);

        report.setModel(Build.MODEL);

        // 如果你的 CrashReport 增加了下面几个字段，可以一并填充
        report.setBrand(Build.BRAND);
        report.setDevice(Build.DEVICE);
        report.setBuildId(Build.DISPLAY);

        if (Build.SUPPORTED_ABIS != null
                && Build.SUPPORTED_ABIS.length > 0) {

            report.setAbi(Build.SUPPORTED_ABIS[0]);

        }

    }

    /**
     * 当前语言
     */
    private static void fillLocale(CrashReport report) {

        Locale locale = Locale.getDefault();

        report.setLocale(locale.toLanguageTag());

    }

}