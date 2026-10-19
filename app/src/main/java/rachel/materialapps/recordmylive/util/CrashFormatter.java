package rachel.materialapps.recordmylive.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import rachel.materialapps.recordmylive.R;

public class CrashFormatter {
    private CrashFormatter() {

    }

    /**
     * 将 Throwable 转换成完整 StackTrace
     */
    public static String getStackTrace(Throwable throwable) {

        if (throwable == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);

        pw.flush();
        pw.close();

        return sw.toString();
    }

    /**
     * 生成最终 Crash Report 文本
     */
    public static String format(CrashReport report) {

        if (report == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("========== " + R.string.app_name + " 崩溃报告 ==========\n\n");

        append(sb, "Crash ID", report.getCrashId());
        append(sb, "崩溃时间", String.valueOf(report.getCrashTime()));

        sb.append('\n');

        append(sb, "应用程序版本", report.getAppVersion());

        append(sb, "版本代号",
                String.valueOf(report.getVersionCode()));

        append(sb, "包名",
                report.getPackageName());

        sb.append('\n');

        append(sb, "Android版本",
                report.getAndroidVersion());

        append(sb,"OS内部版本",
                report.getBuildId());

        append(sb, "API级别",
                String.valueOf(report.getSdkInt()));


        append(sb, "CPU ABI",
                report.getAbi());

        append(sb, "操作系统区域设置",
                report.getLocale());

        sb.append('\n');

        append(sb, "OEM名称",
                report.getManufacturer());

        append(sb, "OEM型号",
                report.getModel());

        append(sb,"OEM品牌名称",
                report.getBrand());

        append(sb,"计算机SKU名称",
                report.getDevice());

        sb.append('\n');

        append(sb, "线程",
                report.getThreadName());

        append(sb, "异常名称",
                report.getExceptionName());

        append(sb, "异常信息",
                report.getExceptionMessage());

        sb.append("\n");

        sb.append("----------------------------------------\n");

        sb.append("堆栈踪迹\n");

        sb.append("----------------------------------------\n\n");

        sb.append(report.getStackTrace());

        return sb.toString();
    }

    /**
     * 输出一行 Key: Value
     */
    private static void append(StringBuilder sb,
                               String key,
                               String value) {

        sb.append(key)
                .append(": ")
                .append(value == null ? "" : value)
                .append('\n');
    }

}
